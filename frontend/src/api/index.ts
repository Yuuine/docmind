import axios from 'axios'
import type { User, Document, ChatSession, ChatMessage, AIModel, AIModelCreateRequest, AIModelUpdateRequest, PageResponse, DocumentStats, DocumentChunkInfo } from '@/types'
import { StreamHttpError } from '@/api/streamError'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 30000
})

api.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const data = response.data
    if (data.code !== 200) {
      return Promise.reject({ response: { data } })
    }
    return data.data
  },
  (error) => Promise.reject(error)
)

declare module 'axios' {
  export interface AxiosInstance {
    request<T = unknown, R = T, D = unknown>(config: AxiosRequestConfig<D>): Promise<R>
    get<T = unknown, R = T, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    delete<T = unknown, R = T, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    head<T = unknown, R = T, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    options<T = unknown, R = T, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    post<T = unknown, R = T, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
    put<T = unknown, R = T, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
    patch<T = unknown, R = T, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
  }
}

export const userApi = {
  register: (data: { username: string; password: string; email?: string; phone?: string }) =>
    api.post<User>('/users/register', data),
  login: (data: { username: string; password: string }) =>
    api.post<User>('/users/login', data),
  getProfile: (userId?: number) => api.get<User>('/users/profile', { params: { userId } }),
  updateProfile: (data: { email?: string; phone?: string; avatarUrl?: string }, userId?: number) =>
    api.put<User>('/users/profile', data, { params: { userId } })
}

export const documentApi = {
  upload: (file: File, userId?: number) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<Document>('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params: { userId }
    })
  },
  list: (
    params: { page?: number; pageSize?: number; filename?: string; status?: string },
    userId?: number,
    signal?: AbortSignal
  ) => api.get<PageResponse<Document>>('/documents', { params: { ...params, userId }, signal }),
  getDetail: (id: number, userId?: number) => api.get<Document>(`/documents/${id}`, { params: { userId } }),
  download: (id: number, userId?: number) => api.get(`/documents/${id}/download`, { responseType: 'blob', params: { userId } }),
  delete: (id: number, userId?: number) => api.delete<{ success: boolean }>(`/documents/${id}`, { params: { userId } }),
  deleteBatch: (ids: number[], userId?: number) =>
    api.post<void>('/documents/batch-delete', { ids }, { params: { userId } }),
  getStats: (id: number, userId?: number) =>
    api.get<DocumentStats>(`/documents/${id}/stats`, { params: { userId } }),
  getChunks: (id: number, userId?: number) =>
    api.get<DocumentChunkInfo[]>(`/documents/${id}/chunks`, { params: { userId } }),
  getChunk: (chunkId: number, userId?: number) =>
    api.get<DocumentChunkInfo>(`/documents/chunks/${chunkId}`, { params: { userId } })
}

export const chatApi = {
  createSession: (data?: { title?: string }, userId?: number) =>
    api.post<ChatSession>('/chat/sessions', data, { params: { userId } }),
  updateSession: (id: number, data: { title?: string }, userId?: number) =>
    api.put<ChatSession>(`/chat/sessions/${id}`, data, { params: { userId } }),
  listSessions: (userId?: number) =>
    api.get<ChatSession[]>('/chat/sessions', { params: { userId } }),
  getMessages: (id: number, userId?: number) =>
    api.get<ChatMessage[]>(`/chat/sessions/${id}/messages`, { params: { userId } }),
  sendMessage: (id: number, data: { content: string }, userId?: number) =>
    api.post<ChatMessage>(`/chat/sessions/${id}/messages`, data, { params: { userId } }),
  sendMessageStream: (
    id: number,
    content: string,
    userId?: number,
    signal?: AbortSignal,
    ragEnabled?: boolean
  ): Promise<ReadableStreamDefaultReader<Uint8Array>> => {
    const params = new URLSearchParams({ content })
    if (userId != null) params.set('userId', String(userId))
    if (ragEnabled === false) params.set('ragEnabled', 'false')
    return fetch(`/api/v1/chat/sessions/${id}/messages/stream?${params.toString()}`, {
      headers: { Accept: 'text/event-stream' },
      signal
    }).then(res => {
      if (!res.ok || !res.body) {
        throw new StreamHttpError(res.status, res.statusText)
      }
      return res.body.getReader()
    })
  },
  deleteSession: (id: number, userId?: number) =>
    api.delete<{ success: boolean }>(`/chat/sessions/${id}`, { params: { userId } })
}

export const modelApi = {
  getModels: (userId: number) =>
    api.get<AIModel[]>('/models', { params: { userId } }),
  createModel: (data: AIModelCreateRequest, userId?: number) =>
    api.post<AIModel>('/models', data, { params: { userId } }),
  updateModel: (id: number, data: AIModelUpdateRequest, userId?: number) =>
    api.put<AIModel>(`/models/${id}`, data, { params: { userId } }),
  deleteModel: (id: number, userId?: number) =>
    api.delete(`/models/${id}`, { params: { userId } }),
  activateModel: (id: number, userId?: number) =>
    api.post(`/models/${id}/activate`, null, { params: { userId } }),
  testConnection: (data: { baseUrl: string; apiKey: string; modelName: string }) =>
    api.post('/models/test-connection', data)
}

export default api
