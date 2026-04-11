"""
BM25 索引模块 - 提供基于关键词的文档检索

使用 rank-bm25 库实现 BM25 算法，支持中文分词（jieba）和英文分词。
索引持久化到磁盘，重启后可恢复。
"""

import logging
import os
import pickle
from typing import List, Dict, Any, Optional

import jieba

try:
    from rank_bm25 import BM25Okapi
except ImportError:
    BM25Okapi = None

from config import config

logger = logging.getLogger(__name__)


class BM25Index:
    """BM25 关键词检索索引

    提供基于 BM25 算法的文档检索功能，支持：
    - 中文分词 (jieba)
    - 英文分词 (空格)
    - 索引持久化

    Attributes:
        index: BM25 索引实例
        corpus: 文档列表
        chunk_id_to_index: chunkId 到索引的映射
        index_to_chunk_id: 索引到 chunkId 的映射
        is_loaded: 索引是否已加载
    """

    def __init__(self, persist_path: str = "./data/bm25_index"):
        """初始化 BM25 索引

        Args:
            persist_path: 索引持久化目录路径
        """
        self.persist_path = persist_path
        self.index_file = os.path.join(persist_path, "bm25_index.pkl")
        self.corpus_file = os.path.join(persist_path, "bm25_corpus.pkl")

        self.index: Optional[BM25Okapi] = None
        self.corpus: List[Dict[str, Any]] = []
        self.chunk_id_to_index: Dict[str, int] = {}
        self.index_to_chunk_id: Dict[int, str] = {}
        self.is_loaded: bool = False

        self._bm25_k1: float = config.BM25_K1
        self._bm25_b: float = config.BM25_B
        self._tokenize_mode: str = config.BM25_TOKENIZE

        self._ensure_persist_dir()
        self._try_load_index()

    def _ensure_persist_dir(self) -> None:
        """确保持久化目录存在"""
        if not os.path.exists(self.persist_path):
            os.makedirs(self.persist_path, exist_ok=True)
            logger.info("Created BM25 persist directory: %s", self.persist_path)

    def _try_load_index(self) -> None:
        """尝试从磁盘加载索引"""
        if not os.path.exists(self.index_file) or not os.path.exists(self.corpus_file):
            logger.info("BM25 index files not found, will build on first use")
            return

        try:
            with open(self.index_file, 'rb') as f:
                self.index = pickle.load(f)
            with open(self.corpus_file, 'rb') as f:
                self.corpus = pickle.load(f)

            self._rebuild_mappings()
            self.is_loaded = True
            logger.info("BM25 index loaded from disk, corpus size: %d", len(self.corpus))
        except Exception as e:
            logger.warning("Failed to load BM25 index from disk: %s", e)
            self.index = None
            self.corpus = []
            self.is_loaded = False

    def save(self) -> None:
        """保存索引到磁盘"""
        if self.index is None:
            logger.warning("No BM25 index to save")
            return

        try:
            with open(self.index_file, 'wb') as f:
                pickle.dump(self.index, f)
            with open(self.corpus_file, 'wb') as f:
                pickle.dump(self.corpus, f)
            logger.info("BM25 index saved to disk, corpus size: %d", len(self.corpus))
        except Exception as e:
            logger.error("Failed to save BM25 index: %s", e)
            raise

    def _rebuild_mappings(self) -> None:
        """重建 chunkId 和索引的映射"""
        self.chunk_id_to_index = {}
        self.index_to_chunk_id = {}
        for i, chunk in enumerate(self.corpus):
            chunk_id = chunk.get("chunkId", f"chunk_{i}")
            self.chunk_id_to_index[chunk_id] = i
            self.index_to_chunk_id[i] = chunk_id

    def _tokenize(self, text: str) -> List[str]:
        """分词

        Args:
            text: 输入文本

        Returns:
            分词后的词列表
        """
        if self._tokenize_mode == "jieba":
            return list(jieba.cut(text))
        elif self._tokenize_mode == "whitespace":
            return text.split()
        else:
            return list(jieba.cut(text))

    def _tokenize_corpus(self, texts: List[str]) -> List[List[str]]:
        """对语料库分词

        Args:
            texts: 文本列表

        Returns:
            分词后的词列表
        """
        return [self._tokenize(text) for text in texts]

    def build_index(self, chunks: List[Dict[str, Any]]) -> None:
        """构建 BM25 索引

        Args:
            chunks: chunk 字典列表，每个包含 chunkId, fileId, content, chunkIndex
        """
        if not chunks:
            logger.warning("Empty chunks list provided to build_index")
            return

        if BM25Okapi is None:
            raise ImportError("rank-bm25 is not installed. Please install: pip install rank-bm25")

        logger.info("Building BM25 index for %d chunks", len(chunks))

        self.corpus = list(chunks)

        contents = [chunk.get("content", "") for chunk in chunks]
        tokenized_contents = self._tokenize_corpus(contents)

        self.index = BM25Okapi(tokenized_contents, k1=self._bm25_k1, b=self._bm25_b)

        self._rebuild_mappings()
        self.is_loaded = True

        logger.info("BM25 index built successfully, corpus size: %d", len(self.corpus))

        self.save()

    def add_chunks(self, chunks: List[Dict[str, Any]]) -> None:
        """添加 chunks 到索引（增量构建）

        注意：BM25 不支持增量添加，需要重新构建整个索引。
        对于大量数据，建议定期重建索引。

        Args:
            chunks: chunk 字典列表
        """
        if not chunks:
            return

        all_chunks = self.corpus + list(chunks)
        self.build_index(all_chunks)

    def update_chunks(self, chunks: List[Dict[str, Any]]) -> None:
        """更新 chunks（重新构建索引）

        Args:
            chunks: chunk 字典列表
        """
        self.build_index(list(chunks))

    def search(self, query: str, top_k: int = 10) -> List[Dict[str, Any]]:
        """搜索相关文档

        Args:
            query: 查询文本
            top_k: 返回的最大结果数量

        Returns:
            搜索结果列表，每个包含 chunkId, fileId, content, score, chunkIndex
        """
        if not query or not query.strip():
            logger.warning("Empty query provided to search")
            return []

        if self.index is None or not self.corpus:
            logger.warning("BM25 index not built yet")
            return []

        logger.debug("BM25 search: query='%s', top_k=%d", query, top_k)

        tokenized_query = self._tokenize(query)
        scores = self.index.get_scores(tokenized_query)

        chunk_scores = []
        for i, score in enumerate(scores):
            if score > 0:
                chunk = self.corpus[i]
                chunk_scores.append((i, score, chunk))

        chunk_scores.sort(key=lambda x: x[1], reverse=True)

        results = []
        for rank, (idx, score, chunk) in enumerate(chunk_scores[:top_k], 1):
            results.append({
                "chunkId": chunk.get("chunkId", self.index_to_chunk_id.get(idx, f"chunk_{idx}")),
                "fileId": chunk.get("fileId", ""),
                "content": chunk.get("content", ""),
                "score": float(score),
                "chunkIndex": chunk.get("chunkIndex", 0),
                "rank": rank
            })

        logger.debug("BM25 search completed: %d results", len(results))
        return results

    def get_stats(self) -> Dict[str, Any]:
        """获取索引统计信息

        Returns:
            统计信息字典
        """
        return {
            "corpus_size": len(self.corpus),
            "is_loaded": self.is_loaded,
            "bm25_k1": self._bm25_k1,
            "bm25_b": self._bm25_b,
            "tokenize_mode": self._tokenize_mode
        }

    def clear(self) -> None:
        """清空索引"""
        self.index = None
        self.corpus = []
        self.chunk_id_to_index = {}
        self.index_to_chunk_id = {}
        self.is_loaded = False

        if os.path.exists(self.index_file):
            os.remove(self.index_file)
        if os.path.exists(self.corpus_file):
            os.remove(self.corpus_file)

        logger.info("BM25 index cleared")


bm25_index = BM25Index()
