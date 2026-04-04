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
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const abortController = ref<AbortController | null>(null)

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

  const sendMessageStream = async (content: string) => {
    if (!userId.value || !currentSessionId.value) return
    abortController.value = new AbortController()
    isSending.value = true
    isStreaming.value = true
    streamingContent.value = ''

    const userMessage: ChatMessage = {
      id: Date.now(),
      sessionId: currentSessionId.value,
      role: 'USER',
      content,
      createdAt: new Date().toISOString()
    }
    messages.value.push(userMessage)

    try {
      const reader = await chatApi.sendMessageStream(
        currentSessionId.value,
        content,
        userId.value
      )
      await processStreamReader(reader)
    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        console.log('Stream aborted by user')
        if (streamingContent.value) {
          messages.value.push({
            id: Date.now() + 1,
            sessionId: currentSessionId.value,
            role: 'ASSISTANT',
            content: streamingContent.value + '\n\n[已中断]',
            createdAt: new Date().toISOString()
          })
        }
      } else {
        console.error('Stream request failed:', error)
      }
    } finally {
      isSending.value = false
      isStreaming.value = false
      streamingContent.value = ''
      abortController.value = null
    }
  }

  const stopStreaming = () => {
    if (abortController.value) {
      abortController.value.abort()
    }
  }

  const processStreamReader = async (reader: ReadableStreamDefaultReader<Uint8Array>): Promise<void> => {
    const decoder = new TextDecoder()
    let buffer = ''

    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split('\n\n')
        buffer = parts.pop() || ''

        for (const part of parts) {
          const trimmed = part.trim()
          if (!trimmed) continue

          const dataLine = trimmed.split('\n').find(line => line.startsWith('data:'))
          if (!dataLine) continue

          const jsonStr = dataLine.slice(5).trim()
          if (!jsonStr) continue

          let parsed
          try {
            parsed = JSON.parse(jsonStr)
          } catch (e) {
            console.warn('Failed to parse SSE JSON:', jsonStr, e)
            continue
          }

          if (parsed.error) {
            console.error('Server stream error:', parsed.error)
            throw new Error(parsed.error)
          }

          if (parsed.done) {
            if (streamingContent.value && userId.value && currentSessionId.value) {
              messages.value.push({
                id: Date.now() + 1,
                sessionId: currentSessionId.value,
                role: 'ASSISTANT',
                content: streamingContent.value,
                createdAt: new Date().toISOString()
              })
            }
            return
          }

          if (parsed.content != null) {
            streamingContent.value += parsed.content
          }
        }
      }

      if (buffer.trim()) {
        const dataLine = buffer.split('\n').find(line => line.startsWith('data:'))
        if (dataLine) {
          const jsonStr = dataLine.slice(5).trim()
          if (jsonStr) {
            try {
              const parsed = JSON.parse(jsonStr)
              if (parsed.content != null) {
                streamingContent.value += parsed.content
              }
            } catch (e) {
              console.warn('Failed to parse remaining SSE JSON:', jsonStr, e)
            }
          }
        }
      }

      if (streamingContent.value && userId.value && currentSessionId.value) {
        messages.value.push({
          id: Date.now() + 1,
          sessionId: currentSessionId.value,
          role: 'ASSISTANT',
          content: streamingContent.value,
          createdAt: new Date().toISOString()
        })
      }
    } finally {
      reader.releaseLock()
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
    isStreaming,
    streamingContent,
    abortController,
    currentSession,

    setUserId,
    loadSessions,
    createSession,
    updateSession,
    switchSession,
    deleteSession,
    sendMessage,
    sendMessageStream,
    stopStreaming,
    processStreamReader,
    setModel,
    toggleCapability,
    setCapabilities
  }
})
