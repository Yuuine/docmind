import axios from 'axios'
import type { ApiResponse, PageResult, User, Document, ChatSession, ChatMessage, AuditLog } from '@/types'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 30000
})

api.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
)

export const userApi = {
  register: (data: { username: string; password: string; email?: string; phone?: string }) =>
    api.post<ApiResponse<User>>('/users/register', data),
  login: (data: { username: string; password: string }) =>
    api.post<ApiResponse<{ userId: number; username: string; avatarUrl?: string }>>('/users/login', data),
  getProfile: () => api.get<ApiResponse<User>>('/users/profile'),
  updateProfile: (data: { email?: string; phone?: string; avatarUrl?: string }) =>
    api.put<ApiResponse<User>>('/users/profile', data)
}

export const documentApi = {
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<ApiResponse<Document>>('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  list: (params: { page: number; size: number }) =>
    api.get<ApiResponse<PageResult<Document>>>('/documents', { params }),
  getDetail: (id: number) => api.get<ApiResponse<Document>>(`/documents/${id}`),
  download: (id: number) => api.get(`/documents/${id}/download`, { responseType: 'blob' }),
  delete: (id: number) => api.delete<ApiResponse<{ success: boolean }>>(`/documents/${id}`)
}

export const chatApi = {
  createSession: (data?: { title?: string }) =>
    api.post<ApiResponse<ChatSession>>('/chat/sessions', data),
  listSessions: (params: { page: number; size: number }) =>
    api.get<ApiResponse<PageResult<ChatSession>>>('/chat/sessions', { params }),
  getMessages: (id: number, params: { page: number; size: number }) =>
    api.get<ApiResponse<PageResult<ChatMessage>>>(`/chat/sessions/${id}/messages`, { params }),
  sendMessage: (id: number, data: { content: string }) =>
    api.post(`/chat/sessions/${id}/messages`, data),
  deleteSession: (id: number) =>
    api.delete<ApiResponse<{ success: boolean }>>(`/chat/sessions/${id}`)
}

export const auditApi = {
  getLogs: (params: {
    page: number
    size: number
    userId?: number
    action?: string
    startDate?: string
    endDate?: string
  }) => api.get<ApiResponse<PageResult<AuditLog>>>('/audit/logs', { params })
}

export default api
