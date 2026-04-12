# DocMindRAG 设计文档

---

## 数据库设计

### users 表
| 字段            | 类型           | 说明    | 约束               |
|---------------|--------------|-------|------------------|
| id            | BIGINT       | 主键    | AUTO_INCREMENT   |
| username      | VARCHAR(50)  | 用户名   | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | 密码哈希  | NOT NULL         |
| email         | VARCHAR(100) | 邮箱    | UNIQUE           |
| phone         | VARCHAR(20)  | 手机号   | UNIQUE           |
| avatar_url    | VARCHAR(500) | 头像URL |                  |
| created_at    | DATETIME     | 创建时间  | NOT NULL         |
| updated_at    | DATETIME     | 更新时间  | NOT NULL         |

### documents 表
| 字段            | 类型           | 说明       | 约束             |
|---------------|--------------|----------|----------------|
| id            | BIGINT       | 主键       | AUTO_INCREMENT |
| user_id       | BIGINT       | 用户ID     | NOT NULL, FK   |
| file_id       | VARCHAR(64)  | 文件ID     | NOT NULL       |
| filename      | VARCHAR(255) | 文件名      | NOT NULL       |
| content_type  | VARCHAR(100) | 内容类型     | NOT NULL       |
| file_size     | BIGINT       | 文件大小(字节) | NOT NULL       |
| file_md5      | VARCHAR(32)  | 文件MD5    |                |
| storage_path  | VARCHAR(500) | 存储路径     | NOT NULL       |
| status        | VARCHAR(20)  | 状态       | NOT NULL       |
| error_message | TEXT         | 错误信息     |                |
| created_at    | DATETIME     | 创建时间     | NOT NULL       |
| updated_at    | DATETIME     | 更新时间     | NOT NULL       |

**索引**:
- `idx_documents_user_created`: 复合索引 (user_id, created_at DESC)，优化列表查询

### document_chunks 表
| 字段           | 类型          | 说明   | 约束             |
|--------------|-------------|------|----------------|
| id           | BIGINT      | 主键   | AUTO_INCREMENT |
| chunk_id     | VARCHAR(64) | 分块ID | NOT NULL, UNIQUE |
| document_id  | BIGINT      | 文档ID | NOT NULL, FK   |
| chunk_index  | INT         | 分块索引 | NOT NULL       |
| content      | TEXT        | 分块内容 | NOT NULL       |
| char_count   | INT         | 字符数  | NOT NULL       |
| created_at   | DATETIME    | 创建时间 | NOT NULL       |

### chat_sessions 表
| 字段         | 类型           | 说明   | 约束             |
|------------|--------------|------|----------------|
| id         | BIGINT       | 主键   | AUTO_INCREMENT |
| user_id    | BIGINT       | 用户ID | NOT NULL, FK   |
| title      | VARCHAR(255) | 会话标题 |                |
| created_at | DATETIME     | 创建时间 | NOT NULL       |
| updated_at | DATETIME     | 更新时间 | NOT NULL       |

### chat_messages 表
| 字段             | 类型          | 说明   | 约束             |
|----------------|-------------|------|----------------|
| id             | BIGINT      | 主键   | AUTO_INCREMENT |
| session_id     | BIGINT      | 会话ID | NOT NULL, FK   |
| role           | VARCHAR(20) | 角色   | NOT NULL       |
| content        | TEXT        | 消息内容 | NOT NULL       |
| retrieved_docs | JSON        | 检索文档 |                |
| created_at     | DATETIME    | 创建时间 | NOT NULL       |

### ai_model 表
| 字段            | 类型           | 说明       | 约束             |
|---------------|--------------|----------|----------------|
| id            | BIGINT       | 主键       | AUTO_INCREMENT |
| user_id       | BIGINT       | 用户ID     | NOT NULL, FK   |
| name          | VARCHAR(100) | 模型名称    | NOT NULL       |
| base_url      | VARCHAR(500) | API 基础地址 | NOT NULL       |
| api_key       | VARCHAR(500) | API 密钥    | NOT NULL       |
| model_name    | VARCHAR(100) | 模型名称    | NOT NULL       |
| max_tokens    | INT          | 最大 token 数 | DEFAULT 4096   |
| temperature   | DOUBLE       | 温度参数    | DEFAULT 0.8    |
| is_active     | BOOLEAN      | 是否激活    | DEFAULT FALSE  |
| provider_type | VARCHAR(30)  | 提供商类型   | DEFAULT 'CUSTOM' |
| extra_config  | TEXT         | 差异化配置   | JSON 格式       |
| created_at    | DATETIME     | 创建时间    | NOT NULL       |
| updated_at    | DATETIME     | 更新时间    | NOT NULL       |

**索引**:
- `idx_user_id`: 用户 ID 索引
- `idx_is_active`: 激活状态索引

### audit_logs 表
| 字段            | 类型           | 说明         | 约束             |
|---------------|--------------|------------|----------------|
| id            | BIGINT       | 主键         | AUTO_INCREMENT |
| user_id       | BIGINT       | 用户ID       | FK             |
| action        | VARCHAR(50)  | 操作类型       | NOT NULL       |
| resource_type | VARCHAR(50)  | 资源类型       |                |
| resource_id   | BIGINT       | 资源ID       |                |
| ip_address    | VARCHAR(45)  | IP地址       |                |
| user_agent    | VARCHAR(500) | User-Agent |                |
| request_data  | JSON         | 请求数据       |                |
| response_data | JSON         | 响应数据       |                |
| status        | VARCHAR(20)  | 状态         | NOT NULL       |
| created_at    | DATETIME     | 创建时间       | NOT NULL       |

---

## 分块策略

系统支持多种文档分块策略，可在 `application.yml` 中配置：

```yaml
docmind:
  document:
    parser:
      chunking-strategy: SENTENCE  # SENTENCE | PARAGRAPH | REGEX | HYBRID | CHARACTER | WORD | LINE
      max-segment-size: 1000       # 最大段落长度
      max-overlap-size: 200         # 重叠大小
      min-chunk-content-length: 50  # 最小分块长度
```

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| SENTENCE | 按句子分块 | 通用文本 |
| PARAGRAPH | 按段落分块 | 结构化文档 |
| REGEX | 按正则表达式分块 | 自定义分隔符 |
| HYBRID | 混合策略 | 智能分块 |
| CHARACTER | 按字符数分块 | 固定长度分块 |
| WORD | 按单词分块 | 英文文档 |
| LINE | 按行分块 | 日志文件 |

---

## API 接口设计

### 用户模块

#### POST /api/v1/users/register
注册新用户
- Request: `{ username, password, email?, phone? }`
- Response: `{ id, username, email, phone, avatarUrl }`

#### POST /api/v1/users/login
用户登录
- Request: `{ username, password }`
- Response: `{ userId, username, avatarUrl }`

#### GET /api/v1/users/profile
获取当前用户信息
- Response: `{ id, username, email, phone, avatarUrl, createdAt }`

#### PUT /api/v1/users/profile
更新用户信息
- Request: `{ email?, phone?, avatarUrl? }`
- Response: `{ id, username, email, phone, avatarUrl }`

### 文档模块

#### POST /api/v1/documents/upload
上传文档
- Request: `multipart/form-data (file, filename?, userId)`
- Response: `{ id, fileId, filename, status }`

#### GET /api/v1/documents
获取文档列表
- Query: `userId, page?, size?`
- Response: `{ items: [{ id, filename, status, createdAt }], total, page, size }`

#### GET /api/v1/documents/{id}
获取文档详情
- Query: `userId`
- Response: `{ id, filename, contentType, fileSize, status, createdAt }`

#### GET /api/v1/documents/{id}/stats
获取文档统计信息
- Query: `userId`
- Response: `{ totalChunks, processedChunks, failedChunks, totalCharacters }`

#### GET /api/v1/documents/{id}/chunks
获取文档分块列表
- Query: `userId`
- Response: `[{ id, chunkId, chunkIndex, contentPreview, charCount, createdAt }]`

#### GET /api/v1/documents/chunks/{chunkId}
获取分块详情
- Query: `userId`
- Response: `{ id, chunkId, chunkIndex, content, charCount, createdAt }`

#### POST /api/v1/documents/{id}/reprocess
重新处理文档
- Query: `userId`
- Response: `{ success }`

#### POST /api/v1/documents/batch-delete
批量删除文档
- Request: `{ ids: [Long] }`
- Query: `userId`
- Response: `{ success }`

#### DELETE /api/v1/documents/{id}
删除文档
- Query: `userId`
- Response: `{ success }`

#### GET /api/v1/documents/{id}/download
下载文档
- Query: `userId`
- Response: 文件流

### 对话模块

#### POST /api/v1/chat/sessions
创建会话
- Request: `{ title? }`
- Query: `userId`
- Response: `{ id, title, createdAt }`

#### GET /api/v1/chat/sessions
获取会话列表
- Query: `userId`
- Response: `[{ id, title, updatedAt }]`

#### PUT /api/v1/chat/sessions/{id}
更新会话
- Request: `{ title }`
- Query: `userId`
- Response: `{ id, title, updatedAt }`

#### GET /api/v1/chat/sessions/{id}/messages
获取会话消息
- Query: `userId`
- Response: `[{ id, role, content, retrievedDocs, createdAt }]`

#### POST /api/v1/chat/sessions/{id}/messages
发送消息
- Request: `{ content, ragEnabled? }`
- Query: `userId`
- Response: `{ id, role, content, retrievedDocs, createdAt }`

#### GET /api/v1/chat/sessions/{id}/messages/stream
发送消息（流式响应，SSE）
- Query: `userId, content, ragEnabled?`
- Response: `text/event-stream`

**SSE 事件格式**:
```
data: {"type":"content","content":"生成的文本"}
data: {"type":"retrieved_docs","docs":[...]}
data: {"type":"done"}
```

#### DELETE /api/v1/chat/sessions/{id}
删除会话
- Query: `userId`
- Response: `{ success }`

### AI 模型模块

#### GET /api/v1/models
获取模型列表
- Query: `userId`
- Response: `[{ id, name, baseUrl, modelName, providerType, isActive, createdAt }]`

#### POST /api/v1/models
创建模型配置
- Request: `{ name, baseUrl, apiKey, modelName, providerType?, maxTokens?, temperature?, extraConfig? }`
- Query: `userId`
- Response: `{ id, name, baseUrl, modelName, providerType, isActive }`

#### PUT /api/v1/models/{id}
更新模型配置
- Request: `{ name?, baseUrl?, apiKey?, modelName?, providerType?, maxTokens?, temperature?, extraConfig? }`
- Query: `userId`
- Response: `{ id, name, baseUrl, modelName, providerType, isActive }`

#### DELETE /api/v1/models/{id}
删除模型配置
- Query: `userId`
- Response: `{ success }`

#### POST /api/v1/models/{id}/activate
激活模型
- Query: `userId`
- Response: `{ success }`

#### POST /api/v1/models/test-connection
测试模型连接
- Request: `{ baseUrl, apiKey, modelName }`
- Response: `{ success, message }`

### 审计日志模块

#### GET /api/v1/audit/logs
获取审计日志（分页）
- Query: `userId?, action?, resourceType?, status?, startDate?, endDate?, page?, pageSize?`
- Response: `{ records: [...], total, page, pageSize, totalPages }`

#### GET /api/v1/audit/logs/{id}
获取单条审计日志
- Response: `{ id, action, resourceType, resourceId, ipAddress, userAgent, status, createdAt }`

---

## 插件系统

系统采用插件化架构，支持扩展以下组件：

### 文档解析插件 (ParserPlugin)
支持多种文档格式：
- `PdfParserPlugin`: PDF 文档
- `WordParserPlugin`: Word 文档 (.docx)
- `ExcelParserPlugin`: Excel 文档 (.xlsx)
- `PptParserPlugin`: PowerPoint 文档 (.pptx)
- `MarkdownParserPlugin`: Markdown 文档
- `TextParserPlugin`: 纯文本文件

### 向量化插件 (EmbeddingPlugin)
- `DefaultEmbeddingPlugin`: 默认实现
- `PythonEmbeddingPlugin`: Python 向量服务实现

### 向量存储插件 (VectorStorePlugin)
- `DefaultVectorStorePlugin`: 默认实现
- `PythonVectorStorePlugin`: Python 向量服务实现

### 重排插件 (RerankPlugin)
- `DefaultRerankPlugin`: 默认实现

### 存储插件 (StoragePlugin)
- 本地文件系统存储

---

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/docmind?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
```

### 聊天 RAG 配置
```yaml
chat:
  rag:
    thread-pool:
      core-size: 4
      max-size: 8
      queue-capacity: 200
      keep-alive-seconds: 60
```

### 审计日志线程池配置
```yaml
audit:
  log:
    thread-pool:
      core-size: 4
      max-size: 10
      queue-capacity: 500
```

### 向量服务配置
```yaml
docmind:
  embedding:
    type: python
    python:
      url: http://localhost:8001
      connect-timeout: 10s
      read-timeout: 30s
  vectorstore:
    python:
      url: http://localhost:8001
      hybrid-enabled: true
```
