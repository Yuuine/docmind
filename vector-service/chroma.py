"""
ChromaDB 向量存储客户端模块
提供文档向量的持久化存储、检索、更新和删除功能
"""

import logging
import asyncio
from typing import List, Dict, Any, Optional

import chromadb
from chromadb.utils import embedding_functions

from config import config
from bm25_index import bm25_index

logger = logging.getLogger(__name__)


class ChromaClient:
    """ChromaDB 向量数据库客户端

    封装了 ChromaDB 的核心操作，包括：
    - 集合的创建和管理
    - 文档向量的增删改查
    - 相似度搜索
    """

    def __init__(self) -> None:
        """初始化 ChromaDB 客户端

        从配置中读取持久化路径、集合名称和嵌入模型名称，
        并自动创建或连接到指定的集合。
        """
        self.persist_path = config.CHROMA_PERSIST_PATH
        self.collection_name = config.CHROMA_COLLECTION_NAME
        self.embedding_model_name = config.EMBEDDING_MODEL_NAME
        self._init_client()

    def _init_client(self) -> None:
        """初始化底层 ChromaDB 客户端和嵌入函数

        创建 PersistentClient 实例，配置 SentenceTransformer 嵌入函数，
        并确保目标集合存在。
        """
        logger.info("Initializing Chroma PersistentClient at: %s", self.persist_path)
        try:
            self.client = chromadb.PersistentClient(path=self.persist_path)

            logger.info("Creating embedding function with model: %s", self.embedding_model_name)
            self.embedding_function = embedding_functions.SentenceTransformerEmbeddingFunction(
                model_name=self.embedding_model_name
            )

            self._ensure_collection_exists()
            logger.info("Chroma client initialized successfully")
        except Exception as e:
            logger.error("Failed to initialize Chroma client: %s", e)
            raise

    def _ensure_collection_exists(self) -> None:
        """确保集合存在

        如果集合已存在则获取引用，否则创建新集合。

        Raises:
            Exception: 当集合创建失败时抛出异常
        """
        try:
            self.collection = self.client.get_collection(
                name=self.collection_name,
                embedding_function=self.embedding_function
            )
            logger.info("Collection %s already exists", self.collection_name)
        except Exception as e:
            logger.info("Collection %s does not exist, creating now: %s", self.collection_name, e)
            try:
                self.collection = self.client.create_collection(
                    name=self.collection_name,
                    embedding_function=self.embedding_function,
                    metadata={"description": "DocMindRAG document chunks"}
                )
                logger.info("Collection %s created successfully", self.collection_name)
            except Exception as create_error:
                logger.error("Failed to create collection %s: %s", self.collection_name, create_error)
                raise

    def add_chunks(self, chunks: List[Dict[str, Any]]) -> None:
        """添加文档块到向量数据库

        Args:
            chunks: chunk字典列表，每个包含chunkId, fileId, content, chunkIndex

        Raises:
            ValueError: 当chunks列表为空时
            Exception: 当数据库操作失败时
        """
        if not chunks:
            logger.warning("Empty chunks list provided to add_chunks")
            raise ValueError("Chunks list cannot be empty")

        logger.info("Starting to add %d chunks to ChromaDB", len(chunks))

        ids = []
        documents = []
        metadatas = []

        for chunk in chunks:
            ids.append(chunk["chunkId"])
            documents.append(chunk["content"])
            metadatas.append({
                "fileId": chunk["fileId"],
                "chunkIndex": str(chunk["chunkIndex"])
            })

        try:
            self.collection.add(
                ids=ids,
                documents=documents,
                metadatas=metadatas
            )
            logger.info("Successfully added %d chunks to ChromaDB", len(chunks))

            bm25_index.add_chunks(chunks)
            logger.info("BM25 index updated with %d chunks", len(chunks))
        except Exception as e:
            logger.error("Failed to add chunks to ChromaDB: %s", e)
            raise

    def update_chunks(self, chunks: List[Dict[str, Any]]) -> None:
        """更新文档块（使用upsert策略）

        如果chunkId已存在则更新，不存在则插入。

        Args:
            chunks: chunk字典列表，每个包含chunkId, fileId, content, chunkIndex

        Raises:
            ValueError: 当chunks列表为空时
            Exception: 当数据库操作失败时
        """
        if not chunks:
            logger.warning("Empty chunks list provided to update_chunks")
            raise ValueError("Chunks list cannot be empty")

        logger.info("Starting to update %d chunks in ChromaDB (upsert)", len(chunks))

        ids = []
        documents = []
        metadatas = []

        for chunk in chunks:
            ids.append(chunk["chunkId"])
            documents.append(chunk["content"])
            metadatas.append({
                "fileId": chunk["fileId"],
                "chunkIndex": str(chunk["chunkIndex"])
            })

        try:
            self.collection.upsert(
                ids=ids,
                documents=documents,
                metadatas=metadatas
            )
            logger.info("Successfully upserted %d chunks to ChromaDB", len(chunks))

            bm25_index.update_chunks(chunks)
            logger.info("BM25 index updated with %d chunks", len(chunks))
        except Exception as e:
            logger.error("Failed to upsert chunks to ChromaDB: %s", e)
            raise

    def search(
        self,
        query: Optional[str] = None,
        query_embedding: Optional[List[float]] = None,
        top_k: int = 10,
        file_ids: Optional[List[str]] = None,
    ) -> List[Dict[str, Any]]:
        """向量相似度搜索

        支持文本查询或向量查询两种模式。

        Args:
            query: 查询文本（可选）
            query_embedding: 查询向量（可选）
            top_k: 返回的最大结果数量，默认为10

        Returns:
            结果列表，每个结果包含chunkId, fileId, content, score, chunkIndex

        Raises:
            ValueError: 当query和query_embedding都未提供时
            Exception: 当数据库操作失败时
        """
        if query is None and query_embedding is None:
            logger.warning("Search called without query or query_embedding")
            raise ValueError("Either query or query_embedding must be provided")

        if file_ids is not None and len(file_ids) == 0:
            logger.info("ChromaDB search: empty file_ids filter, returning no results")
            return []

        logger.info(
            "ChromaDB search: query=%s, has_embedding=%s, top_k=%d, file_id_filter=%s",
            query is not None,
            query_embedding is not None,
            top_k,
            "none" if file_ids is None else len(file_ids),
        )

        where_filter: Optional[Dict[str, Any]] = None
        if file_ids is not None:
            where_filter = {"fileId": {"$in": file_ids}}

        try:
            results = []

            query_kwargs: Dict[str, Any] = {"n_results": top_k}
            if where_filter is not None:
                query_kwargs["where"] = where_filter

            if query is not None:
                query_kwargs["query_texts"] = [query]
                query_results = self.collection.query(**query_kwargs)
            elif query_embedding is not None:
                query_kwargs["query_embeddings"] = [query_embedding]
                query_results = self.collection.query(**query_kwargs)

            if "ids" in query_results and query_results["ids"]:
                ids_list = query_results["ids"][0]
                documents_list = query_results["documents"][0]
                distances_list = query_results["distances"][0]
                metadatas_list = query_results["metadatas"][0]

                for i in range(len(ids_list)):
                    metadata = metadatas_list[i]
                    results.append({
                        "chunkId": ids_list[i],
                        "fileId": metadata["fileId"],
                        "content": documents_list[i],
                        "score": 1.0 - distances_list[i],
                        "chunkIndex": int(metadata.get("chunkIndex", "0"))
                    })

            logger.info("ChromaDB search completed: returning %d results", len(results))
            return results
        except Exception as e:
            logger.error("ChromaDB search failed: %s", e)
            raise

    def hybrid_search(
        self,
        query: Optional[str] = None,
        query_embedding: Optional[List[float]] = None,
        top_k: int = 10,
        file_ids: Optional[List[str]] = None,
        rrf_k: int = 60,
        include_vector_score: bool = True,
        include_bm25_score: bool = True
    ) -> List[Dict[str, Any]]:
        """混合检索：向量检索 + BM25 检索 + RRF 融合

        Args:
            query: 查询文本（可选）
            query_embedding: 查询向量（可选）
            top_k: 返回的最大结果数量
            file_ids: 文件ID过滤列表（可选）
            rrf_k: RRF 融合参数 k
            include_vector_score: 是否在结果中包含向量分数
            include_bm25_score: 是否在结果中包含 BM25 分数

        Returns:
            融合后的结果列表，包含 chunkId, fileId, content, score, vectorScore, bm25Score, chunkIndex
        """
        if query is None and query_embedding is None:
            raise ValueError("Either query or query_embedding must be provided")

        logger.info("Hybrid search: query=%s, top_k=%d, rrf_k=%d",
                    query is not None, top_k, rrf_k)

        vector_results = []
        if query is not None or query_embedding is not None:
            vector_results = self.search(
                query=query,
                query_embedding=query_embedding,
                top_k=top_k,
                file_ids=file_ids
            )

        bm25_results = bm25_index.search(query, top_k=top_k)

        if file_ids is not None:
            bm25_results = [r for r in bm25_results if r.get("fileId") in file_ids]

        fused_results = self._rrf_fusion(
            vector_results,
            bm25_results,
            rrf_k,
            include_vector_score,
            include_bm25_score
        )

        final_results = fused_results[:top_k]

        logger.info("Hybrid search completed: %d results", len(final_results))
        return final_results

    def _rrf_fusion(
        self,
        vector_results: List[Dict[str, Any]],
        bm25_results: List[Dict[str, Any]],
        rrf_k: int = 60,
        include_vector_score: bool = True,
        include_bm25_score: bool = True
    ) -> List[Dict[str, Any]]:
        """RRF (Reciprocal Rank Fusion) 融合

        RRF_score(d) = Σ 1/(k + rank(d))

        Args:
            vector_results: 向量检索结果
            bm25_results: BM25 检索结果
            rrf_k: RRF 融合参数 k
            include_vector_score: 是否包含向量分数
            include_bm25_score: 是否包含 BM25 分数

        Returns:
            融合后的结果列表
        """
        chunk_scores: Dict[str, Dict[str, Any]] = {}

        for rank, result in enumerate(vector_results, 1):
            chunk_id = result["chunkId"]
            if chunk_id not in chunk_scores:
                chunk_scores[chunk_id] = {
                    "chunkId": chunk_id,
                    "fileId": result.get("fileId", ""),
                    "content": result.get("content", ""),
                    "chunkIndex": result.get("chunkIndex", 0),
                    "vectorScore": 0.0,
                    "bm25Score": 0.0,
                    "rrfScore": 0.0
                }
            chunk_scores[chunk_id]["vectorScore"] = result.get("score", 0.0)
            chunk_scores[chunk_id]["rrfScore"] += 1.0 / (rrf_k + rank)

        for rank, result in enumerate(bm25_results, 1):
            chunk_id = result["chunkId"]
            if chunk_id not in chunk_scores:
                chunk_scores[chunk_id] = {
                    "chunkId": chunk_id,
                    "fileId": result.get("fileId", ""),
                    "content": result.get("content", ""),
                    "chunkIndex": result.get("chunkIndex", 0),
                    "vectorScore": 0.0,
                    "bm25Score": 0.0,
                    "rrfScore": 0.0
                }
            chunk_scores[chunk_id]["bm25Score"] = result.get("score", 0.0)
            chunk_scores[chunk_id]["rrfScore"] += 1.0 / (rrf_k + rank)

        results = list(chunk_scores.values())
        results.sort(key=lambda x: x["rrfScore"], reverse=True)

        for result in results:
            result["score"] = result.pop("rrfScore")

            if not include_vector_score:
                result.pop("vectorScore", None)
            if not include_bm25_score:
                result.pop("bm25Score", None)

        return results

    def delete_by_file_id(self, file_id: str) -> None:
        """按文件ID删除所有关联的向量

        Args:
            file_id: 要删除的文件ID

        Raises:
            ValueError: 当file_id为空时
            Exception: 当数据库操作失败时
        """
        if not file_id:
            logger.warning("Empty file_id provided to delete_by_file_id")
            raise ValueError("File ID cannot be empty")

        logger.info("Deleting from ChromaDB by fileId: %s", file_id)

        try:
            self.collection.delete(
                where={"fileId": file_id}
            )
            logger.info("ChromaDB deletion completed: fileId=%s", file_id)
        except Exception as e:
            logger.error("Failed to delete by fileId %s: %s", file_id, e)
            raise

    def delete_collection(self) -> None:
        """删除整个集合并重新创建

        警告：此操作将删除集合中的所有数据！

        Raises:
            Exception: 当删除或重建集合失败时
        """
        logger.warning("Deleting entire ChromaDB collection: %s", self.collection_name)
        try:
            self.client.delete_collection(name=self.collection_name)
            logger.info("ChromaDB collection %s deleted successfully", self.collection_name)

            bm25_index.clear()
            logger.info("BM25 index cleared")

            self._ensure_collection_exists()
        except Exception as e:
            logger.error("Failed to delete ChromaDB collection: %s", e)
            raise


# 全局单例实例
chroma_client = ChromaClient()
