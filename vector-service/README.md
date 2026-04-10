# DocMindRAG Vector Service

这是 DocMindRAG 的向量服务微服务，使用 ChromaDB 内置的 Sentence Transformers 模型进行本地向量化，无需外部 API。

## 功能特性

- 📦 **本地向量化**：使用 `paraphrase-multilingual-MiniLM-L12-v2` 模型，支持中文
- 💾 **持久化存储**：向量数据存储在本地文件系统
- 🔍 **相似度检索**：基于余弦相似度的语义搜索
- 🚀 **完全离线**：无需外部网络，完全本地运行

## 环境配置

1. 复制 `.env.example` 文件为 `.env`：
   ```bash
   copy .env.example .env
   ```

2. 主要配置项说明：
   - `SERVICE_PORT`: 服务端口（默认 8001）
   - `CHROMA_PERSIST_PATH`: ChromaDB 持久化路径（默认 ./data/chroma_db）
   - `CHROMA_COLLECTION_NAME`: Collection 名称（默认 docmind_chunks）
   - `EMBEDDING_MODEL_NAME`: 向量化模型（默认 paraphrase-multilingual-MiniLM-L12-v2）

## 安装依赖

```bash
pip install -r requirements.txt
```

**首次启动会自动下载模型文件**（约 400MB），请确保网络连接正常。

## 启动服务

```bash
python main.py
```

或使用 uvicorn：

```bash
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

服务将在 http://localhost:8001 启动。

## API 端点

- `GET /health` - 健康检查
- `POST /api/v1/vectors/add` - 添加文档块（自动向量化）
- `POST /api/v1/vectors/search` - 相似度搜索
- `POST /api/v1/vectors/delete` - 按文件 ID 删除
- `DELETE /api/v1/vectors/all` - 清空所有数据
