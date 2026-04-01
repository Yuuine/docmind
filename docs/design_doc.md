# DocMindRAG 设计文档

---

## 数据库设计

### users 表
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| username | VARCHAR(50) | 用户名 | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | 密码哈希 | NOT NULL |
| email | VARCHAR(100) | 邮箱 | UNIQUE |
| phone | VARCHAR(20) | 手机号 | UNIQUE |
| avatar_url | VARCHAR(500) | 头像URL | |
| created_at | DATETIME | 创建时间 | NOT NULL |
| updated_at | DATETIME | 更新时间 | NOT NULL |

### documents 表
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | NOT NULL, FK |
| file_id | VARCHAR(64) | 文件ID | NOT NULL |
| filename | VARCHAR(255) | 文件名 | NOT NULL |
| content_type | VARCHAR(100) | 内容类型 | NOT NULL |
| file_size | BIGINT | 文件大小(字节) | NOT NULL |
| file_md5 | VARCHAR(32) | 文件MD5 | |
| storage_path | VARCHAR(500) | 存储路径 | NOT NULL |
| status | VARCHAR(20) | 状态 | NOT NULL |
| error_message | TEXT | 错误信息 | |
| created_at | DATETIME | 创建时间 | NOT NULL |
| updated_at | DATETIME | 更新时间 | NOT NULL |

### document_chunks 表
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| document_id | BIGINT | 文档ID | NOT NULL, FK |
| chunk_index | INT | 分块索引 | NOT NULL |
| content | TEXT | 分块内容 | NOT NULL |
| embedding_id | VARCHAR(64) | 向量ID | |
| created_at | DATETIME | 创建时间 | NOT NULL |

### chat_sessions 表
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | NOT NULL, FK |
| title | VARCHAR(255) | 会话标题 | |
| created_at | DATETIME | 创建时间 | NOT NULL |
| updated_at | DATETIME | 更新时间 | NOT NULL |

### chat_messages 表
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| session_id | BIGINT | 会话ID | NOT NULL, FK |
| role | VARCHAR(20) | 角色 | NOT NULL |
| content | TEXT | 消息内容 | NOT NULL |
| retrieved_docs | JSON | 检索文档 | |
| created_at | DATETIME | 创建时间 | NOT NULL |

### audit_logs 表
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | FK |
| action | VARCHAR(50) | 操作类型 | NOT NULL |
| resource_type | VARCHAR(50) | 资源类型 | |
| resource_id | BIGINT | 资源ID | |
| ip_address | VARCHAR(45) | IP地址 | |
| user_agent | VARCHAR(500) | User-Agent | |
| request_data | JSON | 请求数据 | |
| response_data | JSON | 响应数据 | |
| status | VARCHAR(20) | 状态 | NOT NULL |
| created_at | DATETIME | 创建时间 | NOT NULL |

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
- Request: `multipart/form-data (file)`
- Response: `{ id, fileId, filename, status }`

#### GET /api/v1/documents
获取文档列表
- Query: `page, size`
- Response: `{ items: [{ id, filename, status, createdAt }], total, page, size }`

#### GET /api/v1/documents/{id}
获取文档详情
- Response: `{ id, filename, contentType, fileSize, status, createdAt }`

#### GET /api/v1/documents/{id}/download
下载文档
- Response: 文件流

#### DELETE /api/v1/documents/{id}
删除文档
- Response: `{ success }`

### 对话模块

#### POST /api/v1/chat/sessions
创建会话
- Request: `{ title? }`
- Response: `{ id, title, createdAt }`

#### GET /api/v1/chat/sessions
获取会话列表
- Query: `page, size`
- Response: `{ items: [{ id, title, updatedAt }], total, page, size }`

#### GET /api/v1/chat/sessions/{id}/messages
获取会话消息
- Query: `page, size`
- Response: `{ items: [{ id, role, content, createdAt }], total, page, size }`

#### POST /api/v1/chat/sessions/{id}/messages
发送消息（流式响应）
- Request: `{ content }`
- Response: `text/event-stream`

#### DELETE /api/v1/chat/sessions/{id}
删除会话
- Response: `{ success }`

### 审计日志模块

#### GET /api/v1/audit/logs
获取审计日志
- Query: `page, size, userId?, action?, startDate?, endDate?`
- Response: `{ items: [{ id, action, resourceType, resourceId, status, createdAt }], total, page, size }`
