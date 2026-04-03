import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatApi } from '@/api'
import type { ChatSession, ChatMessage } from '@/types'

interface ChatState {
  sessionId: number | null
  messages: ChatMessage[]
  modelId: number | null
  capabilities: string[]
  isLoading: boolean
}

export const useChatStore = defineStore('chat', () => {
  const userId = ref<number | null>(null)
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const currentModelId = ref<number | null>(null)
  const currentCapabilities = ref<string[]>([])
  const isLoading = ref(false)
  const isSending = ref(false)

  const currentSession = computed(() => {
    return sessions.value.find(s => s.id === currentSessionId.value) || null
  })

  const setUserId = (id: number | null) => {
    userId.value = id
  }

  const loadSessions = async () => {
    if (!userId.value) return
    try {
      const data = await chatApi.listSessions(userId.value)
      sessions.value = data || []
    } catch (error) {
      console.error('Load sessions failed:', error)
    }
  }

  const createSession = async (title?: string) => {
    if (!userId.value) return null
    try {
      const data = await chatApi.createSession({ title }, userId.value)
      sessions.value.unshift(data)
      currentSessionId.value = data.id
      messages.value = []
      return data.id
    } catch (error) {
      console.error('Create session failed:', error)
      return null
    }
  }

  const updateSession = async (id: number, title: string) => {
    if (!userId.value) return
    try {
      const data = await chatApi.updateSession(id, { title }, userId.value)
      const index = sessions.value.findIndex(s => s.id === id)
      if (index !== -1) {
        sessions.value[index] = data
      }
    } catch (error) {
      console.error('Update session failed:', error)
    }
  }

  const switchSession = async (id: number) => {
    if (!userId.value) return
    currentSessionId.value = id
    messages.value = []
    try {
      const data = await chatApi.getMessages(id, userId.value)
      messages.value = data || []
    } catch (error) {
      console.error('Load messages failed:', error)
    }
  }

  const deleteSession = async (id: number) => {
    if (!userId.value) return
    try {
      await chatApi.deleteSession(id, userId.value)
      sessions.value = sessions.value.filter(s => s.id !== id)
      if (currentSessionId.value === id) {
        if (sessions.value.length > 0) {
          await switchSession(sessions.value[0].id)
        } else {
          currentSessionId.value = null
          messages.value = []
        }
      }
    } catch (error) {
      console.error('Delete session failed:', error)
    }
  }

  const sendMessage = async (content: string) => {
    if (!userId.value || !currentSessionId.value) return
    isSending.value = true

    const tempMessage: ChatMessage = {
      id: Date.now(),
      sessionId: currentSessionId.value,
      role: 'USER',
      content,
      createdAt: new Date().toISOString()
    }
    messages.value.push(tempMessage)

    try {
      const data = await chatApi.sendMessage(currentSessionId.value, { content }, userId.value)
      messages.value.push(data)
    } catch (error) {
      console.error('Send message failed:', error)
    } finally {
      isSending.value = false
    }
  }

  const setModel = (modelId: number | null) => {
    currentModelId.value = modelId
  }

  const toggleCapability = (capability: string) => {
    const index = currentCapabilities.value.indexOf(capability)
    if (index === -1) {
      currentCapabilities.value.push(capability)
    } else {
      currentCapabilities.value.splice(index, 1)
    }
  }

  const setCapabilities = (capabilities: string[]) => {
    currentCapabilities.value = capabilities
  }

  return {
    userId,
    sessions,
    currentSessionId,
    messages,
    currentModelId,
    currentCapabilities,
    isLoading,
    isSending,
    currentSession,

    setUserId,
    loadSessions,
    createSession,
    updateSession,
    switchSession,
    deleteSession,
    sendMessage,
    setModel,
    toggleCapability,
    setCapabilities
  }
})
