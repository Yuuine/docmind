<template>
  <div class="chat-page" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <ChatSidebar
      :collapsed="sidebarCollapsed"
      @switch-session="handleSwitchSession"
      @confirm-delete="openDeleteSessionModal"
    />

    <button
      class="sidebar-toggle-btn"
      @click="toggleSidebar"
      :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
    >
      <Icon :name="sidebarCollapsed ? 'panelLeftOpen' : 'panelLeftClose'" :size="14" />
    </button>

    <ChatMessageThread ref="messageThreadRef" @scroll-el="messagesScrollEl = $event" />

    <ChatComposer
      :messages-container="messagesScrollEl ?? null"
      v-model:models-modal="showModelsModal"
      @before-send="onComposerBeforeSend"
    />

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
import { ref, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { useToastStore } from '@/stores/toast'
import { useModelsStore } from '@/stores/models'
import { Icon } from '@/components/icons'
import { ModelsModal } from '@/components/models'
import ConfirmModal from '@/components/ConfirmModal.vue'
import ChatSidebar from '@/components/chat/ChatSidebar.vue'
import ChatMessageThread from '@/components/chat/ChatMessageThread.vue'
import ChatComposer from '@/components/chat/ChatComposer.vue'
import type { ChatSession } from '@/types'

const userStore = useUserStore()
const chatStore = useChatStore()
const toastStore = useToastStore()
const modelStore = useModelsStore()

const userId = userStore.user?.id

const sidebarCollapsed = ref<boolean>(localStorage.getItem('sidebar-collapsed') === 'true')
const showModelsModal = ref(false)
const showDeleteSessionModal = ref(false)
const targetDeleteSession = ref<ChatSession | null>(null)
const messagesScrollEl = ref<HTMLElement | undefined>()
const messageThreadRef = ref<InstanceType<typeof ChatMessageThread> | null>(null)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebar-collapsed', String(sidebarCollapsed.value))
}

async function onComposerBeforeSend(content: string) {
  await messageThreadRef.value?.prepareScrollForOutgoingMessage(content)
}

async function handleSwitchSession(sessionId: number) {
  chatStore.stopStreaming()
  messageThreadRef.value?.prepareScrollForOutgoingMessage()
  messageThreadRef.value?.bumpScrollTop()
  await chatStore.switchSession(sessionId)
  messageThreadRef.value?.scrollToBottomImmediate()
}

function openDeleteSessionModal(session: ChatSession) {
  targetDeleteSession.value = session
  showDeleteSessionModal.value = true
}

async function handleConfirmDeleteSession() {
  if (!targetDeleteSession.value) return
  try {
    await chatStore.deleteSession(targetDeleteSession.value.id)
    toastStore.success('对话已删除')
    messageThreadRef.value?.prepareScrollForOutgoingMessage()
    await nextTick()
    messageThreadRef.value?.scrollToBottomImmediate()
  } catch {
    toastStore.error('删除失败，请稍后重试')
  } finally {
    targetDeleteSession.value = null
  }
}

onMounted(async () => {
  if (userId) {
    chatStore.setUserId(userId)
    await chatStore.loadSessions()
    if (messagesScrollEl.value) {
      messagesScrollEl.value.scrollTop = 999999
    }
    await chatStore.restoreLastSession()
    messageThreadRef.value?.scrollToBottomImmediate()
    modelStore.loadModels(userId)
  }
})
</script>

<style scoped>
.chat-page {
  position: relative;
  height: calc(100vh - 56px);
  display: flex;
  overflow: hidden;
  background: var(--chat-canvas-bg);
  --sidebar-gutter: 8px;
  --sidebar-area: calc(var(--sidebar-gutter) + var(--sidebar-width) + var(--sidebar-gutter));
  --sidebar-animate-duration: 300ms;
  --sidebar-animate-ease: cubic-bezier(0.25, 0.1, 0.25, 1);
}

.chat-page.sidebar-collapsed {
  --sidebar-area: 0px;
}

.chat-page.sidebar-collapsed :deep(.sidebar) {
  pointer-events: none;
}

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
</style>
