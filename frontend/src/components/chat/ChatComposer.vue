<template>
  <div class="chat-input-floating">
    <div class="chat-input-floating-column">
      <div class="chat-scroll-above-input">
        <ScrollToBottom :container="messagesContainer ?? null" />
      </div>
      <div class="chat-input-container">
        <div class="input-field">
          <textarea
            v-model="inputMessage"
            class="input-textarea"
            :class="{ 'input-disabled': chatStore.isStreaming || chatStore.isSending }"
            :placeholder="chatStore.isStreaming ? 'AI正在回复中，请稍候...' : '输入您的问题...'"
            rows="1"
            :disabled="chatStore.isStreaming"
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
              <div v-if="showModelMenu" class="dropdown-menu">
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
                    <Icon
                      v-if="model.id === modelStore.activeModel?.id"
                      name="check"
                      class="check-icon"
                      :size="20"
                    />
                    <div v-else-if="switchingModelId === model.id" class="loading-spinner"></div>
                  </button>
                  <div class="menu-divider"></div>
                  <button class="menu-item add-model-item" @click="openModelsModal">
                    <Icon name="plus" :size="16" />
                    <span>模型管理</span>
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
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { useToastStore } from '@/stores/toast'
import { useModelsStore } from '@/stores/models'
import { Icon } from '@/components/icons'
import ScrollToBottom from '@/components/ScrollToBottom.vue'
import { getProviderLabel } from '@/constants/providers'
import { useClickOutside } from '@/composables/useClickOutside'
import { showSendMessageErrorToast } from '@/utils/errors'
import type { AIModel } from '@/types'

const modelsModalOpen = defineModel<boolean>('modelsModal', { default: false })

defineProps<{
  messagesContainer?: HTMLElement | null
}>()

const emit = defineEmits<{ beforeSend: [content: string] }>()

const userStore = useUserStore()
const chatStore = useChatStore()
const toastStore = useToastStore()
const modelStore = useModelsStore()

const userId = userStore.user?.id

const textareaRef = ref<HTMLTextAreaElement>()
const modelSwitchRef = ref<HTMLElement>()
const inputMessage = ref('')
const showModelMenu = ref(false)
const isDeepThinking = ref(false)
const switchingModelId = ref<number | null>(null)

useClickOutside(modelSwitchRef, () => {
  showModelMenu.value = false
})

function adjustHeight() {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = `${Math.min(textarea.scrollHeight, 160)}px`
  }
}

async function sendMessage() {
  if (!inputMessage.value.trim()) return
  
  if (chatStore.isStreaming) {
    toastStore.warning('AI正在回复中，请等待完成后再发送')
    return
  }
  
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
    emit('beforeSend', content)
    await chatStore.sendMessageStream(content)
  } catch (error) {
    console.error('[ChatComposer] sendMessage error:', error)
    showSendMessageErrorToast(toastStore, error)
  }
}

function toggleModelMenu() {
  showModelMenu.value = !showModelMenu.value
}

async function selectModel(model: AIModel) {
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

function openModelsModal() {
  showModelMenu.value = false
  modelsModalOpen.value = true
}
</script>

<style scoped>
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

.input-textarea.input-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-textarea:disabled {
  cursor: not-allowed;
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
  justify-content: center;
  gap: 6px;
}

.add-model-item:hover {
  color: var(--text-primary);
}

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
