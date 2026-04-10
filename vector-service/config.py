"""
配置模块 - Vector Service 全局配置

从环境变量中读取配置项，提供合理的默认值。
所有配置项都支持通过 .env 文件或环境变量覆盖。
"""

import os
from typing import Optional

from dotenv import load_dotenv

load_dotenv()


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
        EMBEDDING_DEVICE: 模型运行设备 (cpu/cuda)
        LOG_LEVEL: 日志级别 (DEBUG/INFO/WARNING/ERROR)
    """

    # 服务配置
    SERVICE_PORT: int = int(os.getenv("SERVICE_PORT", "8001"))
    SERVICE_HOST: str = os.getenv("SERVICE_HOST", "0.0.0.0")

    # ChromaDB 配置
    CHROMA_PERSIST_PATH: str = os.getenv("CHROMA_PERSIST_PATH", "./data/chroma_db")
    CHROMA_COLLECTION_NAME: str = os.getenv("CHROMA_COLLECTION_NAME", "docmind_chunks")

    # Embedding 模型配置
    EMBEDDING_MODEL_NAME: str = os.getenv(
        "EMBEDDING_MODEL_NAME",
        "paraphrase-multilingual-MiniLM-L12-v2"
    )
    EMBEDDING_DEVICE: str = os.getenv("EMBEDDING_DEVICE", "cpu")  # cpu 或 cuda

    # 日志配置
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")


# 全局配置实例
config = Config()
