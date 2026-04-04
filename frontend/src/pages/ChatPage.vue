<template>
  <div class="chat-page">
    <div class="sidebar" :style="{ width: sidebarCollapsed ? '0px' : '260px' }">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="createNewSession">
          <Icon name="plus" :size="18" />
          <span>新对话</span>
        </button>
        <button class="collapse-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
          <Icon name="panelLeftClose" :size="18" />
        </button>
      </div>

      <div class="session-list">
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

      <div class="user-info" ref="userInfoRef">
        <div class="user-avatar" @click.stop="toggleUserMenu">
          <Icon name="user" :size="20" />
        </div>
        <span class="username" @click.stop="toggleUserMenu">{{ userStore.user?.username }}</span>
        <button class="more-btn" @click.stop="toggleUserMenu">
          <Icon name="more" :size="18" />
        </button>

        <transition name="menu-fade">
          <div v-if="showUserMenu" class="user-menu">
            <div class="menu-item" @click="openSettings">
              <Icon name="settings" :size="16" />
              <span>系统设置</span>
            </div>
            <div class="menu-divider"></div>
            <div class="menu-item logout-item" @click="handleLogout">
              <Icon name="logout" :size="16" />
              <span>退出登录</span>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <div class="main-content">
      <div class="content-toolbar">
        <button v-if="sidebarCollapsed" class="expand-btn" @click="toggleSidebar" title="展开侧边栏">
          <Icon name="panelLeftOpen" :size="18" />
        </button>
        <div class="model-selector-wrapper" ref="modelSelectorRef">
          <div class="model-selector" @click.stop="toggleModelMenu">
            <span class="model-name">{{ modelStore.activeModel?.name || '选择模型' }}</span>
            <span v-if="modelStore.activeModel?.providerType && modelStore.activeModel.providerType !== 'CUSTOM'" class="selector-provider-tag">{{ getProviderLabel(modelStore.activeModel.providerType) }}</span>
            <span class="chevron">▼</span>
          </div>
          <transition name="menu-fade">
            <div v-if="showModelMenu" class="model-dropdown">
              <template v-if="modelStore.models.length === 0">
                <div class="dropdown-item add-model-item" @click="openModelsModal">
                  <Icon name="plus" :size="14" />
                  <span>添加模型</span>
                </div>
              </template>
              <template v-else>
                <div
                  v-for="model in modelStore.models"
                  :key="model.id"
                  :class="['dropdown-item', { active: model.id === modelStore.activeModel?.id }]"
                  @click="selectModel(model.id)"
                >
                  <div class="model-item-info">
                    <span class="model-item-name">{{ model.name }}</span>
                    <span v-if="model.providerType && model.providerType !== 'CUSTOM'" class="provider-tag">{{ getProviderLabel(model.providerType) }}</span>
                  </div>
                  <Icon v-if="model.id === modelStore.activeModel?.id" name="check" :size="14" />
                </div>
                <div class="menu-divider"></div>
                <div class="dropdown-item add-model-item" @click="openModelsModal">
                  <Icon name="plus" :size="14" />
                  <span>添加模型</span>
                </div>
              </template>
            </div>
          </transition>
        </div>
      </div>
      <div class="messages-container" ref="messagesContainer">
        <div v-if="!chatStore.currentSessionId" class="empty-state">
          <Icon name="message" :size="48" />
          <h2>开始新对话</h2>
          <p>创建新对话或选择历史对话开始</p>
        </div>

        <div v-else class="messages-list">
          <div
            v-for="(message, index) in chatStore.messages"
            :key="message.id"
            :class="['message', message.role.toLowerCase()]"
          >
            <div class="message-avatar">
              <Icon v-if="message.role === 'ASSISTANT'" name="logo" :size="20" />
              <Icon v-else name="user" :size="20" />
            </div>
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
            </div>
          </div>

          <div v-if="chatStore.isSending && !chatStore.isStreaming" class="message assistant">
            <div class="message-avatar">
              <Icon name="logo" :size="20" />
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="chatStore.currentSessionId" class="input-area">
        <AutoResizeTextarea
          ref="inputRef"
          v-model="inputMessage"
          placeholder="输入您的问题..."
          :min-rows="1"
          :max-rows="6"
          enter-behavior="send"
          @send="sendMessage"
        >
          <template #suffix>
            <button
              :class="['send-btn', { 'stop-btn': chatStore.isStreaming }]"
              @click="handleSendOrStop"
              :disabled="!inputMessage.trim() && !chatStore.isStreaming"
            >
              <Icon v-if="chatStore.isStreaming" name="stop" :size="16" />
              <Icon v-else name="arrowRight" :size="20" />
            </button>
          </template>
        </AutoResizeTextarea>
      </div>
    </div>

    <div class="upload-panel">
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
          <h4>已上传文件</h4>
          <div v-for="file in uploadedFiles" :key="file.id" class="file-item">
            <Icon name="document" :size="16" />
            <span>{{ file.filename }}</span>
          </div>
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
import { AutoResizeTextarea } from '@/components'
import type { Document, ChatSession } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const toastStore = useToastStore()
const modelStore = useModelsStore()

const messagesContainer = ref<HTMLElement>()
const inputRef = ref<InstanceType<typeof AutoResizeTextarea>>()
const titleInputRef = ref<HTMLInputElement>()
const userInfoRef = ref<HTMLElement>()
const modelSelectorRef = ref<HTMLElement>()
const inputMessage = ref('')
const uploadProgress = ref(0)
const uploadedFiles = ref<Document[]>([])
const editingSessionId = ref<number | null>(null)
const editingTitle = ref('')
const showUserMenu = ref(false)
const showModelMenu = ref(false)
const showModelsModal = ref(false)
const isUserNearBottom = ref(true)
const sidebarCollapsed = ref<boolean>(localStorage.getItem('sidebar-collapsed') === 'true')

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebar-collapsed', String(sidebarCollapsed.value))
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

async function sendMessage() {
  if (!inputMessage.value.trim()) return
  const content = inputMessage.value.trim()
  inputMessage.value = ''
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

  uploadProgress.value = 10
  try {
    const response = await documentApi.upload(file) as any
    uploadProgress.value = 100
    uploadedFiles.value.unshift(response)
    toastStore.success('文件上传成功')
    setTimeout(() => {
      uploadProgress.value = 0
    }, 1500)
  } catch (error) {
    uploadProgress.value = 0
    toastStore.error('文件上传失败')
  }
}

function handleLogout() {
  showUserMenu.value = false
  userStore.logout()
  router.push('/')
}

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function toggleModelMenu() {
  showModelMenu.value = !showModelMenu.value
}

async function selectModel(modelId: number) {
  if (userId) {
    await modelStore.activateModel(modelId, userId)
  }
  showModelMenu.value = false
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
  showUserMenu.value = false
  toastStore.info('系统设置功能开发中')
}

function handleClickOutside(event: MouseEvent) {
  if (userInfoRef.value && !userInfoRef.value.contains(event.target as Node)) {
    showUserMenu.value = false
  }
  if (modelSelectorRef.value && !modelSelectorRef.value.contains(event.target as Node)) {
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
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  background: #faf9f7;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  background: white;
  border-right: 1px solid #e5e5e5;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e5e5e5;
  display: flex;
  align-items: center;
  gap: 8px;
}

.new-chat-btn {
  width: 100%;
  padding: 10px 16px;
  background: black;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.new-chat-btn:hover {
  background: #333;
  transform: translateY(-1px);
}

.collapse-btn {
  padding: 8px;
  background: none;
  border: 1px solid #e5e5e5;
  color: #666;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.collapse-btn:hover {
  background: #f5f5f5;
  color: #1a1a1a;
  border-color: #d0d0d0;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background 0.2s ease;
  margin-bottom: 4px;
}

.session-item:hover {
  background: #f5f5f5;
}

.session-item.active {
  background: #f5f5f5;
  font-weight: 500;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: #1a1a1a;
}

.session-title-input {
  flex: 1;
  font-size: 14px;
  color: #1a1a1a;
  border: 1px solid #1a1a1a;
  border-radius: 4px;
  padding: 4px 8px;
  outline: none;
  background: white;
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.edit-btn,
.delete-btn {
  padding: 4px;
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.edit-btn:hover {
  background: #e5e5e5;
  color: #1a1a1a;
}

.delete-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

.user-info {
  padding: 16px;
  border-top: 1px solid #e5e5e5;
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-avatar:hover {
  background: #e5e5e5;
  color: #1a1a1a;
}

.username {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-btn {
  padding: 6px;
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.more-btn:hover {
  background: #f5f5f5;
  color: #666;
}

.user-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 16px;
  right: 16px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 8px 0;
  z-index: 100;
  min-width: 160px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.2s ease;
}

.menu-item:hover {
  background: #f5f5f5;
  color: #1a1a1a;
}

.menu-item.logout-item {
  color: #ef4444;
}

.menu-item.logout-item:hover {
  background: #fee2e2;
  color: #dc2626;
}

.menu-divider {
  height: 1px;
  background: #e5e5e5;
  margin: 4px 0;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.content-toolbar {
  height: 48px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #e5e5e5;
  background: white;
  flex-shrink: 0;
}

.expand-btn {
  padding: 6px 10px;
  background: none;
  border: 1px solid #e5e5e5;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.expand-btn:hover {
  background: #f5f5f5;
  color: #1a1a1a;
  border-color: #d0d0d0;
}

.model-selector-wrapper {
  position: relative;
  margin-left: 12px;
}

.model-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #1a1a1a;
  background: white;
  transition: all 0.2s ease;
  user-select: none;
}

.model-selector:hover {
  border-color: #d0d0d0;
  background: #faf9f7;
}

.model-name {
  font-weight: 500;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chevron {
  font-size: 10px;
  color: #999;
  transition: transform 0.2s ease;
}

.model-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  min-width: 200px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 6px 0;
  z-index: 200;
  overflow: hidden;
}

.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.15s ease;
}

.dropdown-item:hover {
  background: #f5f5f5;
  color: #1a1a1a;
}

.dropdown-item.active {
  background: #f5f5f5;
  font-weight: 500;
  color: #1a1a1a;
}

.model-item-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-item-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.provider-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
  white-space: nowrap;
  font-weight: 500;
  flex-shrink: 0;
}

.selector-provider-tag {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
  white-space: nowrap;
  font-weight: 500;
  flex-shrink: 0;
}

.dropdown-item.active .model-item-name {
  font-weight: 500;
}

.add-model-item {
  color: #666;
}

.add-model-item:hover {
  color: #1a1a1a;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  gap: 16px;
}

.empty-state h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.empty-state p {
  font-size: 14px;
}

.messages-list {
  max-width: 800px;
  margin: 0 auto;
}

.message {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: black;
  color: white;
}

.message.assistant .message-avatar {
  background: #f5f5f5;
  color: #1a1a1a;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 15px;
}

.message.user .message-text {
  background: black;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-text {
  background: white;
  color: #1a1a1a;
  border: 1px solid #e5e5e5;
  border-bottom-left-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 16px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #666;
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

.input-area {
  padding: 20px 24px;
  border-top: 1px solid #e5e5e5;
  background: white;
}

.send-btn {
  width: 36px;
  height: 36px;
  background: black;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #333;
  transform: translateY(-1px);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn.stop-btn {
  background: #ef4444;
}

.send-btn.stop-btn:hover:not(:disabled) {
  background: #dc2626;
}

.streaming-cursor {
  display: inline-block;
  color: #1a1a1a;
  font-weight: 400;
  margin-left: 1px;
  animation: blink-cursor 0.8s step-end infinite;
}

@keyframes blink-cursor {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.upload-panel {
  width: 240px;
  background: white;
  border-left: 1px solid #e5e5e5;
  display: flex;
  flex-direction: column;
}

.upload-header {
  padding: 20px;
  border-bottom: 1px solid #e5e5e5;
}

.upload-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.upload-content {
  padding: 20px;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  border: 2px dashed #e5e5e5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #666;
  text-align: center;
}

.upload-area:hover {
  border-color: #1a1a1a;
  background: #faf9f7;
}

.upload-area p {
  margin-top: 12px;
  font-size: 14px;
}

.upload-area .hint {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.upload-area input {
  display: none;
}

.progress-bar {
  margin-top: 16px;
  height: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  position: relative;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: black;
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

.uploaded-files h4 {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin-bottom: 12px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #faf9f7;
  border-radius: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #1a1a1a;
}

.file-item span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 菜单动画 */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
