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
  updatedAt: string
}

export type DocumentStatus = 'UPLOADING' | 'PARSING' | 'INDEXING' | 'READY' | 'ERROR'

export interface Document {
  id: number
  userId: number
  fileId: string
  filename: string
  contentType: string
  fileSize: number
  fileMd5?: string
  storagePath: string
  status: DocumentStatus
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

export type ModelProviderType = 'DEEPSEEK' | 'OPENAI' | 'MOONSHOT' | 'QWEN' | 'CUSTOM'

export interface AIModel {
  id: number
  name: string
  baseUrl: string
  apiKey: string
  modelName: string
  maxTokens: number
  temperature: number
  providerType?: ModelProviderType
  extraConfig?: Record<string, any>
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface AIModelCreateRequest {
  name: string
  baseUrl: string
  apiKey: string
  modelName: string
  maxTokens?: number
  temperature?: number
  providerType?: ModelProviderType
  extraConfig?: Record<string, any>
}

export interface AIModelUpdateRequest {
  name?: string
  baseUrl?: string
  apiKey?: string
  modelName?: string
  maxTokens?: number
  temperature?: number
  providerType?: ModelProviderType
  extraConfig?: Record<string, any>
}
