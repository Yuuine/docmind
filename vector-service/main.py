"""
Vector Service - 文档向量微服务
提供文档向量化、存储、检索等功能
"""

import logging
from typing import List, Optional

import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from config import config
from chroma import chroma_client
from embedding import embedding_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

app = FastAPI(title="Vector Service", version="1.0.0")


class Chunk(BaseModel):
    chunkId: str
    fileId: str
    content: str
    chunkIndex: int


class AddChunksRequest(BaseModel):
    chunks: List[Chunk]


class SearchRequest(BaseModel):
    """fileIds, when set, limits search to chunks whose metadata fileId is in the list (per-user document scope)."""

    query: Optional[str] = None
    queryEmbedding: Optional[List[float]] = None
    topK: int = 10
    fileIds: Optional[List[str]] = None


class DeleteRequest(BaseModel):
    fileId: str


class UpdateChunksRequest(BaseModel):
    chunks: List[Chunk]


class EmbeddingRequest(BaseModel):
    text: str

class BatchEmbeddingRequest(BaseModel):
    texts: List[str]

class EmbeddingResponse(BaseModel):
    embedding: List[float]
    dimension: int

class BatchEmbeddingResponse(BaseModel):
    embeddings: List[List[float]]
    dimension: int

class DimensionResponse(BaseModel):
    dimension: int


@app.get("/health")
async def health_check():
    """健康检查端点 - 返回详细的服务状态"""
    health_status = {
        "status": "healthy",
        "service": "vector-service",
        "version": "1.0.0",
        "components": {}
    }

    # 检查 ChromaDB 状态
    try:
        chroma_healthy = chroma_client.collection is not None
        collection_name = chroma_client.collection_name if chroma_healthy else None
        health_status["components"]["chromadb"] = {
            "status": "healthy" if chroma_healthy else "unhealthy",
            "collection": collection_name,
            "persist_path": chroma_client.persist_path
        }
    except Exception as e:
        logger.error("ChromaDB health check failed: %s", e)
        health_status["components"]["chromadb"] = {
            "status": "unhealthy",
            "error": str(e)
        }
        health_status["status"] = "degraded"

    # 检查 Embedding 模型状态
    try:
        model_loaded = embedding_service.is_model_loaded()
        model_name = config.EMBEDDING_MODEL_NAME
        dimension = embedding_service.get_dimension() if model_loaded else None

        health_status["components"]["embedding"] = {
            "status": "ready" if model_loaded else "not_loaded",
            "model": model_name,
            "dimension": dimension,
            "device": config.EMBEDDING_DEVICE
        }

        if not model_loaded:
            health_status["status"] = "degraded"
    except Exception as e:
        logger.error("Embedding health check failed: %s", e)
        health_status["components"]["embedding"] = {
            "status": "error",
            "error": str(e)
        }
        health_status["status"] = "degraded"

    logger.info("Health check requested, status: %s", health_status["status"])
    return health_status


@app.post("/api/v1/embeddings")
async def create_embedding(request: EmbeddingRequest):
    """单个文本向量化"""
    try:
        if not request.text or not request.text.strip():
            logger.warning("Empty text provided for embedding")
            raise ValueError("Text cannot be empty")
        logger.info("Creating embedding for text, length: %d", len(request.text))
        embedding = embedding_service.embed_text(request.text)
        dimension = embedding_service.get_dimension()
        return {
            "status": "success",
            "embedding": embedding,
            "dimension": dimension
        }
    except ValueError as e:
        logger.warning("Embedding parameter error: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("Embedding failed: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/embeddings/batch")
async def create_batch_embeddings(request: BatchEmbeddingRequest):
    """批量文本向量化"""
    try:
        if not request.texts:
            logger.warning("Empty texts list provided for batch embedding")
            raise ValueError("Texts list cannot be empty")
        logger.info("Creating batch embeddings for %d texts", len(request.texts))
        embeddings = embedding_service.embed_texts(request.texts)
        dimension = embedding_service.get_dimension()
        return {
            "status": "success",
            "embeddings": embeddings,
            "dimension": dimension
        }
    except ValueError as e:
        logger.warning("Batch embedding parameter error: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("Batch embedding failed: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/embeddings/dimension")
async def get_embedding_dimension():
    """获取向量维度"""
    try:
        logger.info("Getting embedding dimension")
        dimension = embedding_service.get_dimension()
        return {"status": "success", "dimension": dimension}
    except Exception as e:
        logger.error("Get dimension failed: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/vectors/add")
async def add_vectors(request: AddChunksRequest):
    """添加文档向量"""
    try:
        if not request.chunks:
            logger.warning("Empty chunks list provided")
            raise ValueError("Chunks list cannot be empty")
        logger.info("Adding %d chunks to vector store", len(request.chunks))
        chunks_dicts = [chunk.model_dump() for chunk in request.chunks]
        chroma_client.add_chunks(chunks_dicts)
        return {"status": "success", "message": f"Added {len(request.chunks)} chunks"}
    except ValueError as e:
        logger.warning("Add vectors parameter error: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("Failed to add chunks: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/vectors/search")
async def search_vectors(request: SearchRequest):
    """向量相似度搜索"""
    try:
        logger.info("Searching vectors, topK: %d", request.topK)
        results = chroma_client.search(
            query=request.query,
            query_embedding=request.queryEmbedding,
            top_k=request.topK,
            file_ids=request.fileIds,
        )
        logger.info("Search completed, found %d results", len(results))
        return {"status": "success", "hits": results}
    except ValueError as e:
        logger.warning("Search parameter error: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("Failed to search vectors: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@app.put("/api/v1/vectors/update")
async def update_vectors(request: UpdateChunksRequest):
    """更新文档向量"""
    try:
        if not request.chunks:
            logger.warning("Empty chunks list provided for update")
            raise ValueError("Chunks list cannot be empty")
        logger.info("Updating %d chunks in vector store", len(request.chunks))
        chunks_dicts = [chunk.model_dump() for chunk in request.chunks]
        chroma_client.update_chunks(chunks_dicts)
        return {"status": "success", "message": f"Updated {len(request.chunks)} chunks"}
    except ValueError as e:
        logger.warning("Update vectors parameter error: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("Failed to update chunks: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/vectors/delete")
async def delete_vectors(request: DeleteRequest):
    """按文件ID删除向量"""
    try:
        if not request.fileId:
            logger.warning("Empty fileId provided for deletion")
            raise ValueError("FileId cannot be empty")
        logger.info("Deleting vectors for fileId: %s", request.fileId)
        chroma_client.delete_by_file_id(request.fileId)
        return {"status": "success", "message": f"Deleted vectors for fileId: {request.fileId}"}
    except ValueError as e:
        logger.warning("Delete vectors parameter error: %s", e)
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("Failed to delete vectors: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/vectors/all")
async def delete_all():
    """删除所有向量数据"""
    try:
        logger.warning("Deleting all vectors from collection")
        chroma_client.delete_collection()
        return {"status": "success", "message": "Collection deleted successfully"}
    except Exception as e:
        logger.error("Failed to delete collection: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=config.SERVICE_PORT,
        reload=True
    )
