<template>
  <div class="chat-page" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <div class="sidebar-wrapper">
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-header">
          <button class="new-chat-btn" @click="createNewSession">
            <Icon name="plus" :size="18" />
            <span>新对话</span>
          </button>
        </div>

        <div class="sidebar-sessions">
          <template v-for="group in groupedSessions" :key="group.label">
            <div v-if="group.sessions.length > 0" class="session-group">
              <div class="session-group-label">{{ group.label }}</div>
              <div
                v-for="session in group.sessions"
                :key="session.id"
                :class="['session-item', { active: chatStore.currentSessionId === session.id }]"
              >
                <input
                  v-if="editingSessionId === session.id"
                  v-model="editingTitle"
                  @keydown="handleSessionTitleKeydown($event, session.id)"
                  @blur="saveSessionTitle(session.id)"
                  ref="titleInputRef"
                  class="session-title-input"
                  placeholder="请输入对话名称"
                />
                <div
                  v-else
                  class="session-title"
                  @click="handleSwitchSession(session.id)"
                  @dblclick="startEditingTitle(session)"
                >
                  {{ session.title || '新对话' }}
                </div>
                <div class="session-actions">
                  <button
                    v-if="editingSessionId !== session.id"
                    class="edit-btn"
                    @click.stop="startEditingTitle(session)"
                  >
                    <Icon name="edit" :size="14" />
                  </button>
                  <button
                    class="delete-btn"
                    @click.stop="confirmDeleteSession(session)"
                  >
                    <Icon name="trash" :size="16" />
                  </button>
                </div>
              </div>
            </div>
          </template>
        </div>
      </aside>
    </div>

    <button class="sidebar-toggle-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
      <Icon :name="sidebarCollapsed ? 'panelLeftOpen' : 'panelLeftClose'" :size="14" />
    </button>

    <div class="main-content">
      <div class="messages-container" ref="messagesContainer" @scroll.passive="onMessagesScroll">
        <div class="messages-list">
          <div v-if="!chatStore.currentSessionId" class="empty-state">
            <div class="empty-state-icon">💬</div>
            <h2 class="empty-state-title">开始新对话</h2>
            <p class="empty-state-subtitle">输入问题，让 AI 为你解答</p>
          </div>

          <template v-else>
            <div
              v-for="(message, index) in chatStore.messages"
              :key="message.id"
              :class="['message', message.role.toLowerCase()]"
            >
              <div class="message-content" :class="{ 'message-content--assistant': message.role === 'ASSISTANT' }">
                <template v-if="message.role === 'ASSISTANT'">
                  <AssistantMessageCard>
                    <MarkdownRenderer
                      :content="message.content"
                      :is-streaming="chatStore.isStreaming && isLastAssistantMessage(index)"
                    />
                    <template #footer>
                      <div class="message-meta-row message-meta-row--assistant">
                        <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                        <button type="button" class="action-copy-btn" @click="copyMessage(message.content)">
                          <Icon name="copy" :size="14" />
                        </button>
                      </div>
                    </template>
                  </AssistantMessageCard>
                </template>
                <template v-else>
                  <div class="message-user-stack">
                    <div class="message-text">
                      {{ message.content }}
                    </div>
                    <div class="message-meta-row message-meta-row--user">
                      <button type="button" class="action-copy-btn" @click="copyMessage(message.content)">
                        <Icon name="copy" :size="14" />
                      </button>
                      <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                    </div>
                  </div>
                </template>
              </div>
            </div>

            <div v-if="chatStore.isSending && !chatStore.isStreaming" class="message assistant">
              <div class="message-content message-content--assistant">
                <AssistantMessageCard>
                  <div class="typing-indicator">
                    <span></span><span></span><span></span>
                  </div>
                </AssistantMessageCard>
              </div>
            </div>

            <div v-if="chatStore.streamError" class="message assistant">
              <div class="message-content message-content--assistant">
                <AssistantMessageCard>
                  <div class="message-text stream-error-text">
                    <Icon name="warning" :size="16" /> {{ chatStore.streamError }}
                  </div>
                  <template #footer>
                    <button class="action-dismiss-btn" @click="chatStore.clearStreamError()">知道了</button>
                  </template>
                </AssistantMessageCard>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div class="chat-input-floating">
      <div class="chat-input-floating-column">
        <div class="chat-scroll-above-input">
          <ScrollToBottom :container="messagesContainer" />
        </div>
        <div class="chat-input-container">
          <div class="input-field">
            <textarea
              v-model="inputMessage"
              class="input-textarea"
              placeholder="输入您的问题..."
              rows="1"
              @keydown.enter.exact.prevent="sendMessage"
              @input="adjustHeight"
              ref="textareaRef"
            ></textarea>
          </div>

          <div class="bottom-toolbar">
            <div class="model-switch-wrapper" ref="modelSwitchRef">
            <button
              class="toolbar-btn model-switch-btn"
              :class="{ active: showModelMenu }"
              @click="toggleModelMenu"
              type="button"
            >
              <span>{{ modelStore.activeModel?.name || '选择模型' }}</span>
            </button>

            <Transition name="menu-fade">
              <div v-if="showModelMenu" class="dropdown-menu" ref="menuRef">
                <div class="menu-header">
                  <span class="menu-title">切换模型</span>
                </div>

                <div class="menu-list">
                  <div v-if="modelStore.models.length === 0" class="menu-empty">
                    暂无可用模型，请先添加并启用模型
                  </div>

                  <button
                    v-for="model in modelStore.models"
                    :key="model.id"
                    class="menu-item"
                    :class="{ active: model.id === modelStore.activeModel?.id }"
                    @click="selectModel(model)"
                    :disabled="switchingModelId === model.id"
                  >
                    <div class="model-info">
                      <span class="model-name">{{ model.name }}</span>
                      <span class="model-provider">{{ getProviderLabel(model.providerType) }}</span>
                    </div>
                    <Icon v-if="model.id === modelStore.activeModel?.id" name="check" class="check-icon" :size="20" />
                    <div v-else-if="switchingModelId === model.id" class="loading-spinner"></div>
                  </button>
                  <div class="menu-divider"></div>
                  <button class="menu-item add-model-item" @click="openModelsModal">
                    <Icon name="plus" :size="16" />
                    <span>添加模型</span>
                  </button>
                </div>
              </div>
            </Transition>
            </div>

            <button
            class="toolbar-btn deep-think-btn"
            :class="{ active: isDeepThinking }"
            @click="isDeepThinking = !isDeepThinking"
            type="button"
          >
            <span>深度思考</span>
            </button>

            <Transition name="fade">
              <button
                v-if="chatStore.isStreaming"
                class="stop-btn"
                @click="chatStore.stopStreaming"
                type="button"
              >
                <Icon name="stop" class="stop-icon" :size="18" />
              </button>
              <button
                v-else-if="inputMessage.trim()"
                class="send-btn"
                @click="sendMessage"
                type="button"
              >
                <Icon
                  name="send"
                  class="send-icon"
                  :size="18"
                  fill="currentColor"
                  stroke="none"
                  :stroke-width="0"
                />
              </button>
            </Transition>
          </div>
        </div>
      </div>
    </div>

    <ModelsModal v-model="showModelsModal" />

    <ConfirmModal
      v-model="showDeleteSessionModal"
      title="删除对话"
      :message="`确定要删除对话「${targetDeleteSession?.title || '新对话'}」吗？此操作不可撤销。`"
      confirm-text="确定删除"
      cancel-text="取消"
      :is-destructive="true"
      @confirm="handleConfirmDeleteSession"
    />

  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, watch, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { useToastStore } from '@/stores/toast'
import { useModelsStore } from '@/stores/models'
import { Icon } from '@/components/icons'
import { ModelsModal } from '@/components/models'
import ConfirmModal from '@/components/ConfirmModal.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AssistantMessageCard from '@/components/AssistantMessageCard.vue'
import ScrollToBottom from '@/components/ScrollToBottom.vue'
import type { ChatSession } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const toastStore = useToastStore()
const modelStore = useModelsStore()

const messagesContainer = ref<HTMLElement>()
const textareaRef = ref<HTMLTextAreaElement>()
const menuRef = ref<HTMLElement>()
const modelSwitchRef = ref<HTMLElement>()
const titleInputRef = ref<HTMLInputElement>()
const inputMessage = ref('')
const editingSessionId = ref<number | null>(null)
const editingTitle = ref('')
const showModelMenu = ref(false)
const showModelsModal = ref(false)
const isUserNearBottom = ref(true)
const sidebarCollapsed = ref<boolean>(localStorage.getItem('sidebar-collapsed') === 'true')
const isDeepThinking = ref(false)
const switchingModelId = ref<number | null>(null)
const showDeleteSessionModal = ref(false)
const targetDeleteSession = ref<ChatSession | null>(null)
const isComponentMounted = ref(true)
let scrollBottomRaf = 0

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebar-collapsed', String(sidebarCollapsed.value))
}

const userId = userStore.user?.id

interface SessionGroup {
  label: string
  sessions: ChatSession[]
}

const groupedSessions = computed<SessionGroup[]>(() => {
  const now = new Date()
  const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
  const oneMonthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)

  const withinWeek: ChatSession[] = []
  const withinMonth: ChatSession[] = []
  const beforeMonth: ChatSession[] = []

  const sorted = [...chatStore.sessions].sort((a, b) =>
    new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
  )

  for (const session of sorted) {
    const updatedAt = new Date(session.updatedAt)
    if (updatedAt >= oneWeekAgo) {
      withinWeek.push(session)
    } else if (updatedAt >= oneMonthAgo) {
      withinMonth.push(session)
    } else {
      beforeMonth.push(session)
    }
  }

  return [
    { label: '一周内', sessions: withinWeek },
    { label: '一周前', sessions: withinMonth },
    { label: '一月前', sessions: beforeMonth }
  ]
})

const stopMessagesWatch = watch(() => chatStore.messages, () => {
  smartScrollToBottom()
}, { deep: true })

const stopStreamingWatch = watch(() => chatStore.streamingContent, () => {
  smartScrollToBottom()
})

async function createNewSession() {
  const sessionId = await chatStore.createSession()
  if (sessionId) {
    toastStore.success('新对话已创建')
  }
}

function adjustHeight() {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = `${Math.min(textarea.scrollHeight, 160)}px`
  }
}

async function sendMessage() {
  if (!inputMessage.value.trim()) return
  const content = inputMessage.value.trim()
  inputMessage.value = ''
  showModelMenu.value = false
  nextTick(adjustHeight)
  try {
    const currentUserId = userStore.user?.id
    if (!currentUserId) {
      toastStore.error('请先登录')
      return
    }
    if (!chatStore.currentSessionId) {
      const sessionId = await chatStore.createSession()
      if (!sessionId) {
        toastStore.error('创建对话失败，请重试')
        return
      }
    }
    isUserNearBottom.value = true
    await chatStore.sendMessageStream(content)
  } catch (error) {
    console.error('[ChatPage] sendMessage error:', error)
    const err = error as any
    if (err?.status) {
      if (err.status === 401 || err.status === 403) {
        toastStore.error('登录已过期，请重新登录')
      } else if (err.status === 404) {
        toastStore.error('会话不存在，请刷新页面重试')
      } else if (err.status >= 500) {
        toastStore.error('服务器内部错误，请稍后重试')
      } else {
        toastStore.error(err.message || '请求失败')
      }
    } else {
      const errMsg = err instanceof Error ? err.message : '发送失败'
      if (errMsg.includes('missing userId') || errMsg.includes('missing sessionId')) {
        toastStore.error('会话状态异常，请刷新页面')
      } else if (errMsg.includes('fetch') || errMsg.includes('network') || errMsg.includes('Failed to fetch')) {
        toastStore.error('网络连接失败，请检查网络')
      } else {
        toastStore.error(errMsg)
      }
    }
  }
}

function handleSendOrStop() {
  if (chatStore.isStreaming) {
    chatStore.stopStreaming()
  } else {
    sendMessage()
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}

function toggleModelMenu() {
  showModelMenu.value = !showModelMenu.value
}

async function selectModel(model: any) {
  if (model.id === modelStore.activeModel?.id) {
    showModelMenu.value = false
    return
  }

  switchingModelId.value = model.id
  try {
    if (userId) {
      await modelStore.activateModel(model.id, userId)
    }
    showModelMenu.value = false
  } catch (error) {
    console.error('Failed to switch model', error)
  } finally {
    switchingModelId.value = null
  }
}

function confirmDeleteSession(session: ChatSession) {
  targetDeleteSession.value = session
  showDeleteSessionModal.value = true
}

async function handleConfirmDeleteSession() {
  if (!targetDeleteSession.value) return
  try {
    await chatStore.deleteSession(targetDeleteSession.value.id)
    toastStore.success('对话已删除')
  } catch (error) {
    toastStore.error('删除失败，请稍后重试')
  } finally {
    targetDeleteSession.value = null
  }
}

function getProviderLabel(providerType: string): string {
  const labels: Record<string, string> = {
    DEEPSEEK: 'DeepSeek',
    OPENAI: 'OpenAI',
    MOONSHOT: 'Kimi',
    QWEN: '千问'
  }
  return labels[providerType] || providerType
}

function openModelsModal() {
  showModelMenu.value = false
  showModelsModal.value = true
}

function openSettings() {
  router.push('/settings')
}

function handleClickOutside(event: MouseEvent) {
  if (modelSwitchRef.value && !modelSwitchRef.value.contains(event.target as Node)) {
    showModelMenu.value = false
  }
}

onMounted(async () => {
  if (userId) {
    chatStore.setUserId(userId)
    await chatStore.loadSessions()
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = 999999
    }
    await chatStore.restoreLastSession()
    scrollToBottomImmediate()
    modelStore.loadModels(userId)
  }
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  isComponentMounted.value = false
  if (scrollBottomRaf) cancelAnimationFrame(scrollBottomRaf)
  scrollBottomRaf = 0
  stopMessagesWatch()
  stopStreamingWatch()
  document.removeEventListener('click', handleClickOutside)
})

function onMessagesScroll() {
  const container = messagesContainer.value
  if (!container) return
  const threshold = 150
  const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight
  isUserNearBottom.value = distanceFromBottom < threshold
}

function smartScrollToBottom() {
  nextTick(() => {
    if (scrollBottomRaf) cancelAnimationFrame(scrollBottomRaf)
    scrollBottomRaf = requestAnimationFrame(() => {
      scrollBottomRaf = 0
      if (!isComponentMounted.value) return
      if (!messagesContainer.value) return
      const container = messagesContainer.value
      const threshold = 150
      const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight

      if (chatStore.isStreaming) {
        if (isUserNearBottom.value || distanceFromBottom < threshold) {
          container.scrollTop = container.scrollHeight
        }
      } else {
        isUserNearBottom.value = distanceFromBottom < threshold
        if (isUserNearBottom.value) {
          container.scrollTop = container.scrollHeight
        }
      }
    })
  })
}

async function handleSwitchSession(sessionId: number) {
  chatStore.stopStreaming()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = 999999
  }
  await chatStore.switchSession(sessionId)
  scrollToBottomImmediate()
}

function scrollToBottomImmediate() {
  nextTick(() => {
    if (!messagesContainer.value) return
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  })
}

function scrollToBottomSmooth() {
  nextTick(() => {
    if (!messagesContainer.value) return
    messagesContainer.value.scrollTo({
      top: messagesContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  })
}

function isLastAssistantMessage(index: number): boolean {
  return index === chatStore.messages.length - 1 && chatStore.messages[index]?.role === 'ASSISTANT'
}

function formatTime(dateString: string) {
  const date = new Date(dateString)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

function startEditingTitle(session: ChatSession) {
  editingSessionId.value = session.id
  editingTitle.value = session.title || ''
  nextTick(() => {
    if (titleInputRef.value) {
      titleInputRef.value.focus()
      titleInputRef.value.select()
    }
  })
}

async function saveSessionTitle(sessionId: number) {
  if (!editingSessionId.value) return
  
  const newTitle = editingTitle.value.trim()
  await chatStore.updateSession(sessionId, newTitle || '新对话')
  editingSessionId.value = null
  editingTitle.value = ''
  toastStore.success('对话名称已更新')
}

function handleSessionTitleKeydown(event: KeyboardEvent, sessionId: number) {
  if (event.key === 'Enter') {
    event.preventDefault()
    saveSessionTitle(sessionId)
  } else if (event.key === 'Escape') {
    editingSessionId.value = null
    editingTitle.value = ''
  }
}

async function copyMessage(content: string) {
  try {
    await navigator.clipboard.writeText(content)
    toastStore.success('已复制到剪贴板')
  } catch {
    toastStore.error('复制失败')
  }
}
</script>

<style scoped>
.chat-page {
  position: relative;
  height: calc(100vh - 56px);
  display: flex;
  overflow: hidden;
  background: var(--chat-canvas-bg);
  /* 收起时勿把 --sidebar-gutter 置 0，避免与 width 过渡不同步 */
  --sidebar-gutter: 8px;
  --sidebar-area: calc(var(--sidebar-gutter) + var(--sidebar-width) + var(--sidebar-gutter));
  --sidebar-animate-duration: 300ms;
  --sidebar-animate-ease: cubic-bezier(0.25, 0.1, 0.25, 1);
}

.chat-page.sidebar-collapsed {
  --sidebar-area: 0px;
}

/* ========== Sidebar：窄卡片 + 仅水平展开（wrapper 宽度裁切，无 scale/纵向位移） ========== */
.sidebar-wrapper {
  position: relative;
  flex-shrink: 0;
  width: var(--sidebar-area);
  min-width: 0;
  height: 100%;
  overflow: hidden;
  transition: width var(--sidebar-animate-duration) var(--sidebar-animate-ease);
  z-index: 10;
}

.sidebar {
  box-sizing: border-box;
  width: var(--sidebar-width);
  height: calc(100% - var(--sidebar-gutter) * 2);
  margin: var(--sidebar-gutter) 0 var(--sidebar-gutter) var(--sidebar-gutter);
  background: var(--bg-primary);
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-page.sidebar-collapsed .sidebar {
  pointer-events: none;
}

@media (prefers-color-scheme: dark) {
  .sidebar {
    border-color: var(--border-light);
    box-shadow: var(--shadow-lg);
  }
}

.sidebar-header {
  flex-shrink: 0;
  padding: 14px 12px 10px;
}

.new-chat-btn {
  width: 100%;
  padding: 10px 14px;
  background: var(--btn-primary-bg);
  border: 2px solid var(--btn-primary-bg);
  border-radius: 999px;
  color: var(--btn-primary-text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background-color var(--transition-fast), border-color var(--transition-fast), filter var(--transition-fast), opacity var(--transition-fast);
}

.new-chat-btn:hover {
  background: var(--btn-primary-bg-hover);
  border-color: var(--btn-primary-bg-hover);
  filter: none;
}

.new-chat-btn:active {
  opacity: 0.9;
}

/* ========== Sidebar Sessions List ========== */
.sidebar-sessions {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 10px 14px;
}

.session-group {
  margin-bottom: 8px;
}

.session-group-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-tertiary);
  padding: 8px 10px 6px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.session-item {
  padding: 10px 12px;
  margin-bottom: 6px;
  gap: 8px;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.session-item:hover {
  background: var(--bg-tertiary);
}

.session-item.active {
  background: var(--color-active-bg);
  border-color: var(--border-light);
}

.session-item.active .session-title {
  font-weight: 600;
}

.session-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 1.45;
  color: var(--text-primary);
  font-weight: 500;
}

.session-title-input {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  padding: 6px 10px;
  outline: none;
  background: var(--bg-primary);
  border: 2px solid var(--border-light);
  font-weight: 500;
  transition: border-color var(--transition-fast);
}

.session-title-input:focus {
  border-color: var(--color-accent);
}

.session-actions {
  display: flex;
  gap: 6px;
  align-items: center;
  opacity: 0;
  transition: opacity 0.15s ease;
  flex-shrink: 0;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.edit-btn,
.delete-btn {
  padding: 6px;
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.edit-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.delete-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

/* ========== Sidebar Toggle Button ========== */
.sidebar-toggle-btn {
  position: absolute;
  left: calc(var(--sidebar-area) + 4px);
  top: 16px;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--bg-primary);
  border: 2px solid var(--border-light);
  box-shadow: none;
  cursor: pointer;
  z-index: 11;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition:
    left var(--sidebar-animate-duration) var(--sidebar-animate-ease),
    background var(--transition-fast),
    color var(--transition-fast);
}

.chat-page.sidebar-collapsed .sidebar-toggle-btn {
  left: 8px;
}

.sidebar-toggle-btn:hover {
  background: var(--bg-primary);
  color: var(--text-primary);
}

/* ========== Main Content ========== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: var(--chat-canvas-bg);
  contain: layout style;
}

/* ========== Messages Container ========== */
.messages-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-lg) var(--space-md);
  padding-left: calc(var(--space-md) + var(--chat-pad-left-extra));
  padding-bottom: 160px;
}

.messages-list {
  max-width: var(--message-max-width);
  margin: 0 auto;
  width: 100%;
}

/* ========== Empty State / Welcome Page ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 80px 24px;
  min-height: 400px;
}

.empty-state-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-state-subtitle {
  font-size: 14px;
  color: var(--text-tertiary);
  max-width: 360px;
  line-height: 1.5;
}

/* ========== Message Bubbles ========== */
.message {
  display: flex;
  margin-bottom: 28px;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-content {
  max-width: var(--message-max-width);
  width: 100%;
}

.message-content--assistant {
  max-width: var(--message-max-width);
}

.message.user .message-content {
  max-width: 70%;
  text-align: right;
}

.message-user-stack {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  max-width: 100%;
  vertical-align: top;
}

.message-text {
  padding: 12px 16px;
  line-height: 1.7;
  font-size: 16px;
  word-wrap: break-word;
  display: inline-block;
}

.message.user .message-text {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg) var(--radius-lg) var(--radius-sm) var(--radius-lg);
  box-shadow: none;
}

.message.assistant .message-text {
  color: var(--text-primary);
  padding: 0;
  display: block;
  width: 100%;
  text-align: left;
}

.message-time {
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-tertiary);
  margin-top: 0;
  white-space: nowrap;
}

/* 时间与复制同一行；助手：时间左、复制右；用户：复制左、时间右（整行贴右对齐） */
.message-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
  min-height: 24px;
}

.message-meta-row--assistant {
  justify-content: flex-start;
  flex-wrap: wrap;
}

.message-meta-row--user {
  justify-content: flex-end;
  width: 100%;
  align-self: stretch;
}

.action-copy-btn {
  padding: 2px 8px;
  min-height: 24px;
  box-sizing: border-box;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-tertiary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}

.action-copy-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

/* ========== Typing Indicator ========== */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 16px;
}

.message-content--assistant .typing-indicator {
  padding: 4px 0;
  min-height: 28px;
  align-items: center;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--text-secondary);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.stream-error-text {
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-dismiss-btn {
  margin-top: 8px;
  padding: 4px 14px;
  background: transparent;
  border: 2px solid var(--border-light);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-dismiss-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

/* ========== 悬浮输入框（顶层，无底部遮罩条） ========== */
.chat-input-floating {
  position: fixed;
  z-index: 100;
  left: var(--sidebar-area);
  right: 0;
  bottom: max(20px, env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  padding-left: calc(var(--space-md) + var(--chat-pad-left-extra) - 14px);
  padding-right: var(--space-md);
  box-sizing: border-box;
  pointer-events: none;
  transition: left var(--sidebar-animate-duration) var(--sidebar-animate-ease);
}

.chat-input-floating-column {
  pointer-events: none;
  width: 100%;
  max-width: var(--message-max-width);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}

.chat-scroll-above-input {
  pointer-events: auto;
  display: flex;
  justify-content: center;
  width: 100%;
}

.chat-page.sidebar-collapsed .chat-input-floating {
  left: 0;
}

.chat-input-container {
  position: relative;
  width: 100%;
  max-width: var(--message-max-width);
  pointer-events: auto;
  background: var(--bg-primary);
  border: 1px solid var(--border-lighter);
  border-radius: var(--chat-input-radius);
  box-shadow:
    0 2px 12px rgba(0, 0, 0, 0.05),
    0 6px 20px -4px rgba(0, 0, 0, 0.07);
  /* 不可 overflow:hidden：模型下拉向上展开会被裁切 */
  overflow: visible;
}

@media (prefers-color-scheme: dark) {
  .chat-input-container {
    border-color: var(--border-light);
    box-shadow:
      0 2px 16px rgba(0, 0, 0, 0.35),
      0 8px 28px -6px rgba(0, 0, 0, 0.45);
  }
}

.input-field {
  position: relative;
  padding: 16px 20px;
  min-height: 52px;
  display: flex;
  align-items: center;
}

.input-textarea {
  display: block;
  width: 100%;
  padding-right: 20px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  max-height: 160px;
  color: var(--text-primary);
  font-family: inherit;
  margin: 0;
}

.input-textarea::placeholder {
  color: var(--text-tertiary);
}

.bottom-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px 12px;
  border-top: 1px solid var(--border-lighter);
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border: 1px solid var(--border-lighter);
  border-radius: 999px;
  background: transparent;
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
  font-size: 13px;
  color: var(--text-secondary);
}

.toolbar-btn:hover:not(:disabled):not(.active) {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border-color: var(--border-light);
}

.toolbar-btn:hover:not(:disabled).active {
  background: var(--color-active-bg);
  color: var(--color-active-text);
  border-color: color-mix(in srgb, var(--color-accent) 35%, var(--border-light));
}

.toolbar-btn:active:not(:disabled) {
  filter: brightness(0.96);
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-btn.active {
  background: var(--color-active-bg);
  color: var(--color-active-text);
  border-color: color-mix(in srgb, var(--color-accent) 40%, var(--border-light));
}

.model-switch-wrapper {
  position: relative;
  z-index: 20;
}

.send-btn,
.stop-btn {
  margin-left: auto;
  flex-shrink: 0;
  box-sizing: border-box;
  width: 32px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  border-style: solid;
  border-width: 1px;
  transition: background-color 0.15s ease, border-color 0.15s ease, filter 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}

.send-btn {
  border-color: var(--btn-primary-bg);
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
}

.send-btn:hover:not(:disabled) {
  background: var(--btn-primary-bg-hover);
  border-color: var(--btn-primary-bg-hover);
  filter: none;
}

.send-btn:active:not(:disabled) {
  filter: brightness(0.94);
}

.send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.stop-btn {
  border-color: var(--btn-danger-bg);
  background: var(--btn-danger-bg);
  color: var(--btn-danger-text);
}

.stop-btn:hover:not(:disabled) {
  background: var(--btn-danger-bg-hover);
  border-color: var(--btn-danger-bg-hover);
  filter: none;
}

.stop-btn:active:not(:disabled) {
  filter: brightness(0.95);
}

.send-btn :deep(svg),
.stop-btn :deep(svg) {
  display: block;
  flex-shrink: 0;
}

.stop-icon,
.send-icon {
  color: inherit;
}

.dropdown-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 0;
  width: 280px;
  background: var(--bg-primary);
  border: 1px solid var(--border-lighter);
  border-radius: 18px;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.08),
    0 12px 32px -8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 300;
}

.menu-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-lighter);
  background: var(--bg-secondary);
}

.menu-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.menu-list {
  max-height: 240px;
  overflow-y: auto;
}

.menu-empty {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-tertiary);
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background 0.15s ease;
  text-align: left;
}

.menu-item:hover:not(:disabled) {
  background: var(--bg-tertiary);
}

.menu-item:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.menu-item.active {
  background: var(--color-active-bg);
}

.model-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.model-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-provider {
  font-size: 12px;
  color: var(--text-tertiary);
}

.check-icon {
  width: 20px;
  height: 20px;
  color: var(--color-accent);
  flex-shrink: 0;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-lighter);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.menu-divider {
  height: 1px;
  background: var(--border-lighter);
  margin: 4px 0;
}

.add-model-item {
  color: var(--text-secondary);
}

.add-model-item:hover {
  color: var(--text-primary);
}

/* ========== Menu Transition ========== */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
