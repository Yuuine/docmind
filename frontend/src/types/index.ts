export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface User {
  id: number
  username: string
  email?: string
  phone?: string
  avatarUrl?: string
  createdAt: string
}

export interface Document {
  id: number
  userId: number
  fileId: string
  filename: string
  contentType: string
  fileSize: number
  fileMd5?: string
  storagePath: string
  status: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface ChatSession {
  id: number
  userId: number
  title?: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  sessionId: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  retrievedDocs?: any
  createdAt: string
}

export interface AuditLog {
  id: number
  userId?: number
  action: string
  resourceType?: string
  resourceId?: number
  ipAddress?: string
  userAgent?: string
  requestData?: any
  responseData?: any
  status: string
  createdAt: string
}
