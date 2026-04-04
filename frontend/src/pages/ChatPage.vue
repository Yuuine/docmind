<template>
  <div class="chat-page" :class="{ 'sidebar-collapsed': sidebarCollapsed, 'upload-collapsed': uploadCollapsed }">
    <div class="sidebar-wrapper">
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-header">
          <button class="new-chat-btn" @click="createNewSession">
            <Icon name="plus" :size="18" />
            <span>新对话</span>
          </button>
        </div>

        <div class="sidebar-sessions">
          <div
            v-for="session in chatStore.sessions"
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
                @click.stop="chatStore.deleteSession(session.id)"
              >
                <Icon name="trash" :size="16" />
              </button>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <button class="sidebar-toggle-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
      <Icon :name="sidebarCollapsed ? 'panelLeftOpen' : 'panelLeftClose'" :size="14" />
    </button>

    <div class="main-content">
      <div class="messages-container" ref="messagesContainer">
        <div class="messages-list">
          <div v-if="!chatStore.currentSessionId" class="empty-state">
            <div class="empty-state-icon">📄</div>
            <h2 class="empty-state-title">开始新对话</h2>
            <p class="empty-state-subtitle">上传文档或输入问题，让 AI 为你解答</p>
          </div>

          <template v-else>
            <div
              v-for="(message, index) in chatStore.messages"
              :key="message.id"
              :class="['message', message.role.toLowerCase()]"
            >
              <div class="message-content">
                <div class="message-text">
                  <template v-if="isLastAssistantMessage(message, index) && chatStore.isStreaming">
                    {{ chatStore.streamingContent }}<span class="streaming-cursor">▊</span>
                  </template>
                  <template v-else>
                    {{ message.content }}
                  </template>
                </div>
                <div class="message-time">{{ formatTime(message.createdAt) }}</div>
                <div v-if="message.role === 'ASSISTANT'" class="message-actions-footer">
                  <button class="action-copy-btn" @click="copyMessage(message.content)">
                    <Icon name="copy" :size="14" /> 复制
                  </button>
                </div>
              </div>
            </div>

            <div v-if="chatStore.isSending && !chatStore.isStreaming" class="message assistant">
              <div class="message-content">
                <div class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div class="upload-panel" :class="{ collapsed: uploadCollapsed }">
      <button class="upload-toggle-btn" @click="toggleUpload" :title="uploadCollapsed ? '展开上传面板' : '收起上传面板'">
        <Icon :name="uploadCollapsed ? 'panelRightOpen' : 'panelRightClose'" :size="14" />
      </button>
      <div v-if="!uploadCollapsed" class="upload-content-wrapper">
        <div class="upload-header">
          <h3>上传文件</h3>
        </div>

        <div class="upload-content">
          <label class="upload-area">
            <input
              type="file"
              @change="handleFileSelect"
              accept=".pdf,.doc,.docx,.txt,.md"
            />
            <Icon name="upload" :size="32" />
            <p>点击或拖拽文件</p>
            <p class="hint">支持 PDF, DOC, DOCX, TXT, MD</p>
          </label>

          <div v-if="uploadProgress > 0" class="progress-bar">
            <div class="progress-fill" :style="{ width: `${uploadProgress}%` }"></div>
            <span>{{ uploadProgress }}%</span>
          </div>

          <div v-if="uploadedFiles.length > 0" class="uploaded-files">
            <div class="uploaded-files-header">
              <h4>已上传文件</h4>
              <router-link to="/documents" class="view-all-link">查看全部 →</router-link>
            </div>
            <DocumentProgress
              v-for="file in uploadedFiles"
              :key="file.id"
              :document="file"
              :refresh-fn="() => refreshFileStatus(file)"
              @remove="removeUploadedFile"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-wrapper" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
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
              v-if="inputMessage.trim()"
              class="send-btn"
              @click="sendMessage"
              type="button"
            >
              <Icon name="arrowUp" class="send-icon" :size="18" />
            </button>
          </Transition>
        </div>
      </div>
    </div>

    <ModelsModal v-model="showModelsModal" />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, watch, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { useToastStore } from '@/stores/toast'
import { useModelsStore } from '@/stores/models'
import { documentApi } from '@/api'
import { Icon } from '@/components/icons'
import { ModelsModal } from '@/components/models'
import DocumentProgress from '@/components/DocumentProgress.vue'
import type { Document, ChatSession } from '@/types'

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
const uploadProgress = ref(0)
const uploadedFiles = ref<Document[]>([])
const editingSessionId = ref<number | null>(null)
const editingTitle = ref('')
const showModelMenu = ref(false)
const showModelsModal = ref(false)
const isUserNearBottom = ref(true)
const sidebarCollapsed = ref<boolean>(localStorage.getItem('sidebar-collapsed') === 'true')
const uploadCollapsed = ref<boolean>(localStorage.getItem('upload-collapsed') === 'true')
const isDeepThinking = ref(false)
const switchingModelId = ref<number | null>(null)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebar-collapsed', String(sidebarCollapsed.value))
}

function toggleUpload() {
  uploadCollapsed.value = !uploadCollapsed.value
  localStorage.setItem('upload-collapsed', String(uploadCollapsed.value))
}

const userId = userStore.user?.id

watch(() => chatStore.messages, () => {
  smartScrollToBottom()
}, { deep: true })

watch(() => chatStore.streamingContent, () => {
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
  await chatStore.sendMessageStream(content)
}

function handleSendOrStop() {
  if (chatStore.isStreaming) {
    chatStore.stopStreaming()
  } else {
    sendMessage()
  }
}

function isLastAssistantMessage(message: any, index: number): boolean {
  return message.role === 'ASSISTANT' && index === chatStore.messages.length - 1
}

async function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    const response = await documentApi.upload(file) as any
    const doc = {
      ...response,
      status: response.status || 'UPLOADING'
    }
    uploadedFiles.value.unshift(doc)
    toastStore.success('文件已提交处理')
  } catch (error) {
    toastStore.error('文件上传失败')
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}

async function refreshFileStatus(file: Document) {
  try {
    const updated = await documentApi.getDetail(file.id)
    const idx = uploadedFiles.value.findIndex(f => f.id === file.id)
    if (idx >= 0 && updated) {
      uploadedFiles.value[idx] = { ...updated }
    }
  } catch {
  }
}

function removeUploadedFile(id: number) {
  uploadedFiles.value = uploadedFiles.value.filter(f => f.id !== id)
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

onMounted(() => {
  if (userId) {
    chatStore.setUserId(userId)
    chatStore.loadSessions()
    modelStore.loadModels(userId)
  }
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

function smartScrollToBottom() {
  nextTick(() => {
    if (!messagesContainer.value) return
    const container = messagesContainer.value
    const threshold = 100
    const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight
    isUserNearBottom.value = distanceFromBottom < threshold
    if (isUserNearBottom.value) {
      container.scrollTop = container.scrollHeight
    }
  })
}

function handleSwitchSession(sessionId: number) {
  chatStore.stopStreaming()
  chatStore.switchSession(sessionId)
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
  background: #faf9f7;
}

/* ========== Sidebar Wrapper & Toggle ========== */
.sidebar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.sidebar {
  width: var(--sidebar-width);
  height: 100%;
  background: var(--bg-primary);
  border-right: 1px solid var(--border-lighter);
  display: flex;
  flex-direction: column;
  transition: transform var(--transition-normal), width var(--transition-normal);
  z-index: 10;
}

.chat-page.sidebar-collapsed .sidebar {
  transform: translateX(-100%);
  width: 0;
  overflow: hidden;
}

.sidebar-header {
  padding: 12px 16px;
}

.new-chat-btn {
  width: 100%;
  padding: 10px 16px;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 20px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all var(--transition-fast);
}

.new-chat-btn:hover {
  background: var(--bg-secondary);
  border-color: #d0d0d0;
}

/* ========== Sidebar Sessions List ========== */
.sidebar-sessions {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.session-item {
  padding: 10px 16px;
  margin: 2px 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background var(--transition-fast);
}

.session-item:hover {
  background: var(--bg-secondary);
}

.session-item.active {
  background: var(--color-active-bg);
}

.session-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  background: var(--color-accent);
  border-radius: 0 2px 2px 0;
}

.session-item.active .session-title {
  color: var(--color-active-text);
  font-weight: 500;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--text-primary);
}

.session-title-input {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  padding: 4px 8px;
  outline: none;
  background: white;
  border: 1px solid var(--border-light);
}

.session-title-input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.08);
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s ease;
  flex-shrink: 0;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.edit-btn,
.delete-btn {
  padding: 4px;
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.edit-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.delete-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

/* ========== Sidebar Toggle Button ========== */
.sidebar-toggle-btn {
  position: absolute;
  left: calc(var(--sidebar-width) + 4px);
  top: 15px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  z-index: 11;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: left var(--transition-normal), background var(--transition-fast), color var(--transition-fast);
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
  background: #faf9f7;
}

/* ========== Messages Container ========== */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg) var(--space-md);
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
  margin-bottom: var(--space-lg);
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-avatar {
  display: none;
}

.message-content {
  max-width: 72%;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  line-height: 1.6;
  font-size: 15px;
  word-wrap: break-word;
  display: inline-block;
}

.message.user .message-text {
  background: var(--bubble-user-bg);
  color: var(--bubble-user-text);
  border-radius: 18px 4px 18px 18px;
  box-shadow: var(--shadow-sm);
}

.message.assistant .message-text {
  background: var(--bubble-assistant-bg);
  color: var(--bubble-assistant-text);
  border-radius: 4px 18px 18px 18px;
  box-shadow: var(--shadow-sm);
}

.message-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 6px;
}

/* ========== Message Actions Footer (Assistant only) ========== */
.message-actions-footer {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.15s ease;
  padding-left: 4px;
}

.message.assistant:hover .message-actions-footer {
  opacity: 1;
}

.action-copy-btn {
  padding: 4px 10px;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all var(--transition-fast);
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

/* ========== Streaming Cursor Animation ========== */
.streaming-cursor {
  display: inline-block;
  color: var(--text-primary);
  font-weight: 400;
  margin-left: 1px;
  animation: blink-cursor 0.8s step-end infinite;
}

@keyframes blink-cursor {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ========== Floating Chat Input ========== */
.chat-input-wrapper {
  position: fixed;
  bottom: 24px;
  left: 280px;
  right: 264px;
  display: flex;
  justify-content: center;
  padding: 0 24px;
  pointer-events: none;
  z-index: 50;
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1), right 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chat-page.sidebar-collapsed .chat-input-wrapper {
  left: 72px;
}

.chat-page.upload-collapsed .chat-input-wrapper {
  right: 24px;
}

.chat-input-container {
  position: relative;
  width: 100%;
  max-width: 816px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
  pointer-events: auto;
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
  border-top: 1px solid #f3f4f6;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #6b7280;
}

.toolbar-btn:hover:not(:disabled):not(.active) {
  background: #f9fafb;
  color: #374151;
  border-color: #d1d5db;
}

.toolbar-btn:hover:not(:disabled).active {
  background: #dbeafe;
  color: #1d4ed8;
  border-color: #2563eb;
}

.toolbar-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-btn.active {
  background: #eff6ff;
  color: #2563eb;
  border-color: #3b82f6;
}

.model-switch-wrapper {
  position: relative;
}

.send-btn {
  margin-left: auto;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #000000;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #333333;
  transform: scale(1.05);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-icon {
  width: 18px;
  height: 18px;
  color: #ffffff;
}

.dropdown-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 0;
  width: 280px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  z-index: 100;
}

.menu-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  background: #fafafa;
}

.menu-title {
  font-size: 13px;
  font-weight: 600;
  color: #333333;
}

.menu-list {
  max-height: 240px;
  overflow-y: auto;
}

.menu-empty {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
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
  background: #f9fafb;
}

.menu-item:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.menu-item.active {
  background: #eff6ff;
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
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-provider {
  font-size: 12px;
  color: #9ca3af;
}

.check-icon {
  width: 20px;
  height: 20px;
  color: #3b82f6;
  flex-shrink: 0;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e5e7eb;
  border-top-color: #3b82f6;
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
  background: #f3f4f6;
  margin: 4px 0;
}

.add-model-item {
  color: #666;
}

.add-model-item:hover {
  color: #1a1a1a;
}

/* ========== Upload Panel ========== */
.upload-panel {
  position: relative;
  width: 240px;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-left: 12px;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal), margin-left var(--transition-normal);
}

.upload-panel.collapsed {
  width: 0;
  margin-left: 0;
  overflow: hidden;
}

.upload-toggle-btn {
  position: absolute;
  right: calc(100% + 4px);
  top: 15px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  z-index: 11;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: right var(--transition-normal), background var(--transition-fast), color var(--transition-fast);
}

.chat-page.upload-collapsed .upload-toggle-btn {
  right: 8px;
}

.upload-toggle-btn:hover {
  background: var(--bg-primary);
  color: var(--text-primary);
}

.upload-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.upload-header {
  padding: 20px;
}

.upload-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.upload-content {
  padding: 0 20px 20px;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  border: 1.5px dashed var(--border-light);
  border-radius: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--text-secondary);
  text-align: center;
}

.upload-area:hover {
  background: rgba(0, 0, 0, 0.02);
  border-color: var(--color-accent);
}

.upload-area p {
  margin-top: 12px;
  font-size: 14px;
}

.upload-area .hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.upload-area input {
  display: none;
}

.progress-bar {
  margin-top: 16px;
  height: 8px;
  background: var(--bg-tertiary);
  border-radius: 4px;
  position: relative;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--text-primary);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-bar span {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: 500;
  color: white;
}

.uploaded-files {
  margin-top: 24px;
}

.uploaded-files-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.uploaded-files h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.view-all-link {
  font-size: 12px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.view-all-link:hover {
  color: var(--text-primary);
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-primary);
}

.file-item span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
