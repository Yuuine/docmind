"""
配置模块 - Vector Service 全局配置

从环境变量中读取配置项，提供合理的默认值。
所有配置项都支持通过 .env 文件或环境变量覆盖。
"""

import os
from typing import Optional

from dotenv import load_dotenv

load_dotenv()


def _check_cuda_available() -> bool:
    """检测 CUDA 是否可用"""
    try:
        import torch
        return torch.cuda.is_available()
    except Exception:
        return False


def _get_device_from_config() -> str:
    """根据配置获取实际运行设备

    Returns:
        str: 实际设备标识，"cpu" 或 "cuda"
    """
    device_config = os.getenv("EMBEDDING_DEVICE", "cpu").lower().strip()

    if device_config == "cpu":
        return "cpu"

    if device_config == "cuda":
        if _check_cuda_available():
            return "cuda"
        else:
            return "cpu"

    if device_config == "auto":
        if _check_cuda_available():
            return "cuda"
        else:
            return "cpu"

    return "cpu"


class Config:
    """Vector Service 配置类

    集中管理所有服务配置项，包括：
    - 服务端口和主机地址
    - ChromaDB 持久化路径和集合名称
    - Embedding 模型名称和设备
    - 日志级别

    Attributes:
        SERVICE_PORT: 服务监听端口，默认8001
        SERVICE_HOST: 服务绑定地址，默认0.0.0.0
        CHROMA_PERSIST_PATH: ChromaDB 数据持久化路径
        CHROMA_COLLECTION_NAME: ChromaDB 集合名称
        EMBEDDING_MODEL_NAME: SentenceTransformer 模型名称
        EMBEDDING_DEVICE: 模型运行设备 (cpu/cuda/auto)
        EMBEDDING_DEVICE_ACTUAL: 实际运行的设备（自动选择）
        LOG_LEVEL: 日志级别 (DEBUG/INFO/WARNING/ERROR)
    """

    SERVICE_PORT: int = int(os.getenv("SERVICE_PORT", "8001"))
    SERVICE_HOST: str = os.getenv("SERVICE_HOST", "0.0.0.0")

    CHROMA_PERSIST_PATH: str = os.getenv("CHROMA_PERSIST_PATH", "./data/chroma_db")
    CHROMA_COLLECTION_NAME: str = os.getenv("CHROMA_COLLECTION_NAME", "docmind_chunks")

    EMBEDDING_MODEL_NAME: str = os.getenv(
        "EMBEDDING_MODEL_NAME",
        "paraphrase-multilingual-MiniLM-L12-v2"
    )

    EMBEDDING_DEVICE: str = os.getenv("EMBEDDING_DEVICE", "cpu")

    EMBEDDING_DEVICE_ACTUAL: str = _get_device_from_config()

    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

    HYBRID_SEARCH_ENABLED: bool = os.getenv("HYBRID_SEARCH_ENABLED", "false").lower() == "true"

    HYBRID_VECTOR_TOP_K: int = int(os.getenv("HYBRID_VECTOR_TOP_K", "10"))
    HYBRID_BM25_TOP_K: int = int(os.getenv("HYBRID_BM25_TOP_K", "10"))
    HYBRID_RRF_K: int = int(os.getenv("HYBRID_RRF_K", "60"))

    BM25_K1: float = float(os.getenv("BM25_K1", "1.5"))
    BM25_B: float = float(os.getenv("BM25_B", "0.75"))
    BM25_TOKENIZE: str = os.getenv("BM25_TOKENIZE", "jieba")


config = Config()
