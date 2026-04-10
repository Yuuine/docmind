"""
Embedding模块 - 使用本地sentence-transformers模型进行文本向量化

提供单个文本和批量文本的向量化功能，支持懒加载模式，
仅在首次调用时加载模型以节省内存资源。
"""

import logging
from typing import List, Optional

from sentence_transformers import SentenceTransformer

from config import config

logger = logging.getLogger(__name__)


class EmbeddingService:
    """本地Embedding服务，使用sentence-transformers模型

    采用类变量实现单例模式，支持懒加载，
    仅在首次调用时初始化模型实例。

    Attributes:
        _model: SentenceTransformer 模型实例（懒加载）
        _dimension: 向量维度（模型加载后确定）
    """

    _model: Optional[SentenceTransformer] = None
    _dimension: Optional[int] = None

    @classmethod
    def _load_model(cls) -> SentenceTransformer:
        """懒加载模型，首次调用时初始化

        从配置中读取模型名称并加载 SentenceTransformer 模型。
        加载成功后缓存模型实例和向量维度。

        Returns:
            SentenceTransformer: 已加载的模型实例

        Raises:
            Exception: 当模型加载失败时
        """
        if cls._model is None:
            logger.info("Loading embedding model: %s", config.EMBEDDING_MODEL_NAME)
            try:
                cls._model = SentenceTransformer(config.EMBEDDING_MODEL_NAME)
                cls._dimension = cls._model.get_sentence_embedding_dimension()
                logger.info("Model loaded successfully, dimension: %d", cls._dimension)
            except Exception as e:
                logger.error("Failed to load embedding model %s: %s",
                            config.EMBEDDING_MODEL_NAME, e)
                raise
        return cls._model

    @classmethod
    def embed_text(cls, text: str) -> List[float]:
        """对单个文本进行向量化

        Args:
            text: 输入文本，不能为空字符串

        Returns:
            向量数组 (float列表)

        Raises:
            ValueError: 当文本为空或仅包含空白字符时
            Exception: 当向量化过程失败时
        """
        if not text or not text.strip():
            logger.warning("Empty text provided for embedding")
            raise ValueError("Text cannot be empty or whitespace only")

        logger.debug("Embedding text, length: %d", len(text))
        try:
            model = cls._load_model()
            embedding = model.encode(text)
            return embedding.tolist()
        except Exception as e:
            logger.error("Failed to embed text: %s", e)
            raise

    @classmethod
    def embed_texts(cls, texts: List[str]) -> List[List[float]]:
        """对批量文本进行向量化

        Args:
            texts: 输入文本列表，不能为空列表

        Returns:
            向量数组列表

        Raises:
            ValueError: 当文本列表为空时
            Exception: 当向量化过程失败时
        """
        if not texts:
            logger.warning("Empty texts list provided for batch embedding")
            raise ValueError("Texts list cannot be empty")

        logger.info("Batch embedding %d texts", len(texts))
        try:
            model = cls._load_model()
            embeddings = model.encode(texts)
            return [emb.tolist() for emb in embeddings]
        except Exception as e:
            logger.error("Failed to batch embed texts: %s", e)
            raise

    @classmethod
    def get_dimension(cls) -> int:
        """获取向量维度

        如果模型尚未加载，会触发模型加载过程。

        Returns:
            向量维度数

        Raises:
            Exception: 当获取维度失败时
        """
        if cls._dimension is None:
            cls._load_model()
        return cls._dimension

    @classmethod
    def is_model_loaded(cls) -> bool:
        """检查模型是否已加载到内存

        Returns:
            bool: 如果模型已加载返回True，否则返回False
        """
        return cls._model is not None


# 全局单例实例
embedding_service = EmbeddingService()
