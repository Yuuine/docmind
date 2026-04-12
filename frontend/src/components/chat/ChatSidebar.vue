<template>
  <div class="sidebar-wrapper">
    <aside class="sidebar" :class="{ collapsed: collapsed }">
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
                @click="emit('switchSession', session.id)"
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
                <button class="delete-btn" @click.stop="emit('confirmDelete', session)">
                  <Icon name="trash" :size="16" />
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { useChatStore } from '@/stores/chat'
import { useToastStore } from '@/stores/toast'
import { Icon } from '@/components/icons'
import { useGroupedSessions } from '@/composables/useGroupedSessions'
import type { ChatSession } from '@/types'

defineProps<{ collapsed: boolean }>()

const emit = defineEmits<{
  switchSession: [sessionId: number]
  confirmDelete: [session: ChatSession]
}>()

const chatStore = useChatStore()
const toastStore = useToastStore()
const { sessions } = storeToRefs(chatStore)
const groupedSessions = useGroupedSessions(sessions)

const titleInputRef = ref<HTMLInputElement>()
const editingSessionId = ref<number | null>(null)
const editingTitle = ref('')

async function createNewSession() {
  const sessionId = await chatStore.createSession()
  if (sessionId) {
    toastStore.success('新对话已创建')
  }
}

function startEditingTitle(session: ChatSession) {
  editingSessionId.value = session.id
  editingTitle.value = session.title || ''
  nextTick(() => {
    const el = titleInputRef.value
    if (el) {
      el.focus()
      el.select()
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
</style>
