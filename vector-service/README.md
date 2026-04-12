# Vector Service

向量嵌入微服务，为 DocMindRAG 系统提供文档向量化存储和检索功能。

## 功能特性

- **文本向量化**: 使用 sentence-transformers 模型将文本转换为高维向量
- **向量存储**: 基于 ChromaDB 的持久化向量数据库
- **相似度检索**: 支持快速的向量相似度搜索
- **混合检索**: 支持向量检索 + BM25 检索 + RRF 融合
- **批量处理**: 支持批量文本嵌入和向量操作
- **懒加载**: Embedding 模型仅在首次调用时加载，节省内存

## 快速开始

### 1. 安装依赖

**方式一：使用安装脚本（Windows，推荐）**

```bash
# 双击运行或终端执行
install.bat
```

**方式二：手动安装**

```bash
# 创建虚拟环境
python -m venv venv

# 激活虚拟环境
# Windows:
call venv\Scripts\activate.bat
# Linux/Mac:
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt
```

### 2. 配置环境变量

创建 `.env` 文件或设置环境变量：

```env
# 服务配置
SERVICE_PORT=8001
SERVICE_HOST=0.0.0.0

# ChromaDB 配置
CHROMA_PERSIST_PATH=./data/chroma_db
CHROMA_COLLECTION_NAME=docmind_chunks

# Embedding 模型配置
EMBEDDING_MODEL_NAME=paraphrase-multilingual-MiniLM-L12-v2
EMBEDDING_DEVICE=cpu

# BM25 检索配置
BM25_TOKENIZE=jieba
BM25_K1=1.5
BM25_B=0.75

# 混合检索 RRF 参数
HYBRID_RRF_K=60

# 日志级别
LOG_LEVEL=INFO
```

### 3. 启动服务

```bash
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

服务启动后，访问 http://localhost:8001/docs 查看 API 文档。

## API 接口

### 健康检查

```bash
GET /health
```

返回服务状态和组件信息：

```json
{
  "status": "healthy",
  "service": "vector-service",
  "version": "1.0.0",
  "components": {
    "chromadb": {"status": "healthy", "collection": "docmind_chunks"},
    "embedding": {"status": "ready", "model": "paraphrase-multilingual-MiniLM-L12-v2", "dimension": 384},
    "bm25": {"status": "ready", "corpus_size": 1000, "k1": 1.5, "b": 0.75}
  }
}
```

### 文本向量化

**单个文本嵌入：**

```bash
POST /api/v1/embeddings
Content-Type: application/json

{
  "text": "要向量化的文本内容"
}
```

**批量文本嵌入：**

```bash
POST /api/v1/embeddings/batch
Content-Type: application/json

{
  "texts": ["文本1", "文本2", "文本3"]
}
```

**获取向量维度：**

```bash
GET /api/v1/embeddings/dimension
```

### 向量存储操作

**添加向量：**

```bash
POST /api/v1/vectors/add
Content-Type: application/json

{
  "chunks": [
    {
      "chunkId": "chunk-001",
      "fileId": "file-001",
      "content": "文档内容片段",
      "chunkIndex": 0
    }
  ]
}
```

**搜索相似向量：**

```bash
POST /api/v1/vectors/search
Content-Type: application/json

{
  "query": "搜索关键词",
  "topK": 5,
  "fileIds": ["file-001", "file-002"],
  "hybrid": true
}
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `query` | String | 是 | - | 搜索关键词 |
| `queryEmbedding` | List[float] | 否 | - | 直接传入向量进行搜索 |
| `topK` | Integer | 否 | 10 | 返回前 K 条结果 |
| `fileIds` | List[string] | 否 | - | 按文件 ID 过滤，限定搜索范围 |
| `hybrid` | Boolean | 否 | false | 是否使用混合检索模式 |

**混合检索（显式调用）：**

```bash
POST /api/v1/vectors/hybrid-search
Content-Type: application/json

{
  "query": "搜索关键词",
  "topK": 5,
  "fileIds": ["file-001"],
  "includeVectorScore": true,
  "includeBm25Score": true
}
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `query` | String | 是 | - | 搜索关键词 |
| `queryEmbedding` | List[float] | 否 | - | 直接传入向量进行搜索 |
| `topK` | Integer | 否 | 5 | 返回前 K 条结果 |
| `fileIds` | List[string] | 否 | - | 按文件 ID 过滤 |
| `includeVectorScore` | Boolean | 否 | true | 是否包含向量检索分数 |
| `includeBm25Score` | Boolean | 否 | true | 是否包含 BM25 检索分数 |

**更新向量：**

```bash
PUT /api/v1/vectors/update
Content-Type: application/json

{
  "chunks": [
    {
      "chunkId": "chunk-001",
      "fileId": "file-001",
      "content": "更新后的内容",
      "chunkIndex": 0
    }
  ]
}
```

**删除向量：**

```bash
POST /api/v1/vectors/delete
Content-Type: application/json

{
  "fileId": "file-001"
}
```

**清空所有向量：**

```bash
DELETE /api/v1/vectors/all
```

## 配置说明

### 基础配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `SERVICE_PORT` | 8001 | 服务监听端口 |
| `SERVICE_HOST` | 0.0.0.0 | 服务绑定地址 |
| `CHROMA_PERSIST_PATH` | ./data/chroma_db | ChromaDB 数据存储路径 |
| `CHROMA_COLLECTION_NAME` | docmind_chunks | 向量集合名称 |
| `EMBEDDING_MODEL_NAME` | paraphrase-multilingual-MiniLM-L12-v2 | SentenceTransformer 模型名称 |
| `LOG_LEVEL` | INFO | 日志级别 |

### 设备配置 (EMBEDDING_DEVICE)

| 配置值 | 说明 | 性能 |
|--------|------|------|
| `cpu` | 强制使用 CPU（默认） | 慢，适合轻度使用 |
| `cuda` | 使用 NVIDIA GPU（需 GPU 环境） | 快，适合大量文档处理 |
| `auto` | 自动检测，优先使用 GPU | 自动选择最佳设备 |

#### GPU 环境要求

使用 `cuda` 模式需要：

1. **NVIDIA GPU 显卡**
2. **安装 NVIDIA CUDA 驱动**
3. **安装支持 CUDA 的 PyTorch 版本**：

```bash
# 安装 CUDA 版本的 PyTorch（以 CUDA 12.1 为例）
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu121
```

#### 性能参考

以 `paraphrase-multilingual-MiniLM-L12-v2` 模型为例：

- **CPU (Intel i7-12700)**: ~100-200 文本/秒
- **GPU (NVIDIA RTX 3080)**: ~1000-2000 文本/秒

#### 验证 CUDA 支持

运行项目提供的 CUDA 检测脚本：

```bash
python check_cuda.py
```

预期输出（CUDA 可用时）：

```
PyTorch 版本: 2.7.1+cu118
CUDA 是否可用: True
PyTorch 编译时使用的 CUDA 版本: 11.8
cuDNN 版本: 90100
当前显卡名称: NVIDIA GeForce RTX 3060
显卡数量: 1
```

#### 故障排除

**配置了 cuda 但仍使用 CPU？**

检查日志中是否有以下信息：

```
Device config: requested=cuda, available=cpu, actual=cpu
CUDA was requested but not available, using CPU instead
```

这表示 CUDA 不可用，可能原因：

1. 未安装 NVIDIA GPU 驱动
2. PyTorch 是 CPU 版本，需要重新安装 CUDA 版本
3. CUDA 版本不兼容

**验证 PyTorch CUDA 支持：**

```bash
python -c "import torch; print('CUDA available:', torch.cuda.is_available())"
```

### BM25 检索配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `BM25_TOKENIZE` | jieba | 分词模式：jieba / space |
| `BM25_K1` | 1.5 | BM25 K1 参数 |
| `BM25_B` | 0.75 | BM25 B 参数 |

### 混合检索配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `HYBRID_RRF_K` | 60 | RRF (Reciprocal Rank Fusion) 融合参数 |

## 依赖版本

已验证兼容的依赖版本：

- fastapi==0.115.0
- uvicorn==0.32.0
- pydantic==2.12.0
- python-dotenv==1.0.1
- chromadb==1.3.0
- sentence-transformers==3.2.0
- transformers==4.46.0
- tokenizers==0.20.0
- numpy==1.26.4
- scipy==1.13.0
- torch==2.7.1+cu118
- torchvision==0.22.1+cu118
- jieba==0.42.1
- protobuf>=3.20.3,<4.0.0
- setuptools>=65.5.0

## 故障排除

### 依赖安装失败

```bash
# 删除现有虚拟环境
rmdir /s /q venv

# 创建新虚拟环境
python -m venv venv

# 激活并安装
call venv\Scripts\activate.bat
pip install --upgrade pip
pip install -r requirements.txt
```

### 端口被占用

```bash
# 修改 .env 文件使用其他端口
SERVICE_PORT=8002

# 重新启动
uvicorn main:app --host 0.0.0.0 --port 8002 --reload
```

### 模型下载失败

确保网络连接正常，首次启动会自动下载模型。如需离线使用，可预先下载模型到本地目录。

## 项目结构

```
vector-service/
├── main.py              # FastAPI 应用入口
├── config.py            # 配置管理
├── chroma.py            # ChromaDB 客户端封装
├── embedding.py         # Embedding 服务封装
├── bm25_index.py        # BM25 索引实现
├── requirements.txt     # Python 依赖清单
├── install.bat          # Windows 安装脚本
├── check_cuda.py        # CUDA 检测脚本
├── README.md            # 项目文档
└── data/                # ChromaDB 数据存储目录
    └── chroma_db/
```

## 开发指南

### 启动开发服务器

```bash
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

### 运行测试

服务启动后，可使用 curl 或 Postman 测试各 API 端点。

### 注意事项

- **不要**在全局 Python 环境中安装项目依赖
- 始终使用虚拟环境隔离项目依赖
- 如遇版本冲突，优先使用虚拟环境而非修改全局包
