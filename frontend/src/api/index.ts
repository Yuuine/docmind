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
  (response) => {
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
    request<T = any, R = T, D = any>(config: AxiosRequestConfig<D>): Promise<R>
    get<T = any, R = T, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    delete<T = any, R = T, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    head<T = any, R = T, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    options<T = any, R = T, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    post<T = any, R = T, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
    put<T = any, R = T, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
    patch<T = any, R = T, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
  }
}

export const userApi = {
  register: (data: { username: string; password: string; email?: string; phone?: string }) =>
    api.post<User>('/users/register', data),
  login: (data: { username: string; password: string }) =>
    api.post<User>('/users/login', data),
  getProfile: () => api.get<User>('/users/profile'),
  updateProfile: (data: { email?: string; phone?: string; avatarUrl?: string }) =>
    api.put<User>('/users/profile', data)
}

export const documentApi = {
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<Document>('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  list: (params: { page: number; size: number }) =>
    api.get<PageResult<Document>>('/documents', { params }),
  getDetail: (id: number) => api.get<Document>(`/documents/${id}`),
  download: (id: number) => api.get(`/documents/${id}/download`, { responseType: 'blob' }),
  delete: (id: number) => api.delete<{ success: boolean }>(`/documents/${id}`)
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
  deleteSession: (id: number, userId?: number) =>
    api.delete<{ success: boolean }>(`/chat/sessions/${id}`, { params: { userId } })
}

export const auditApi = {
  getLogs: (params: {
    page: number
    size: number
    userId?: number
    action?: string
    startDate?: string
    endDate?: string
  }) => api.get<PageResult<AuditLog>>('/audit/logs', { params })
}

export default api
