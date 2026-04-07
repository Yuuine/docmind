<template>
  <div class="main-content">
    <div
      class="messages-container"
      ref="messagesContainerRef"
      @scroll.passive="onMessagesScroll"
    >
      <div class="messages-list">
        <div v-if="!chatStore.currentSessionId" class="empty-state">
          <div class="empty-state-icon">💬</div>
          <h2 class="empty-state-title">开始新对话</h2>
          <p class="empty-state-subtitle">输入问题，让 AI 为你解答</p>
        </div>

        <template v-else>
          <!-- 按会话重建虚拟列表实例，清空 TanStack 测量缓存，避免切换/删除会话后行间重叠 -->
          <ChatSessionVirtualRows
            :key="chatStore.currentSessionId"
            :get-scroll-parent="getScrollParent"
          />

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
                  <button class="action-dismiss-btn" @click="chatStore.clearStreamError()">
                    知道了
                  </button>
                </template>
              </AssistantMessageCard>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useChatScroll } from '@/composables/useChatScroll'
import AssistantMessageCard from '@/components/AssistantMessageCard.vue'
import ChatSessionVirtualRows from '@/components/chat/ChatSessionVirtualRows.vue'
import { Icon } from '@/components/icons'

const emit = defineEmits<{ scrollEl: [el: HTMLElement | undefined] }>()

const chatStore = useChatStore()

const messagesContainerRef = ref<HTMLElement>()

function getScrollParent() {
  return messagesContainerRef.value ?? null
}

watch(messagesContainerRef, (v) => emit('scrollEl', v), { immediate: true })

const { onMessagesScroll, scrollToBottomImmediate, prepareScrollForOutgoingMessage, scrollAfterMessageSent } = useChatScroll({
  scrollEl: messagesContainerRef,
  isStreaming: () => chatStore.isStreaming,
  getMessages: () => chatStore.messages,
  getStreamingContent: () => chatStore.streamingContent
})

function bumpScrollTop() {
  const c = messagesContainerRef.value
  if (c) c.scrollTop = 999999
}

async function prepareScrollForOutgoingMessageAsync(userContent?: string) {
  prepareScrollForOutgoingMessage()
  await scrollAfterMessageSent(userContent)
}

defineExpose({
  scrollToBottomImmediate,
  bumpScrollTop,
  prepareScrollForOutgoingMessage: prepareScrollForOutgoingMessageAsync
})
</script>

<style scoped>
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: var(--chat-canvas-bg);
  contain: layout style;
}

.messages-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--space-lg) var(--space-md);
  padding-left: calc(var(--space-md) + var(--chat-pad-left-extra));
  /* 为固定输入条预留足够留白，避免最后一条消息被挡在输入框后（含多行输入与工具栏） */
  padding-bottom: 220px;
}

.messages-list {
  max-width: var(--message-max-width);
  margin: 0 auto;
  width: 100%;
}

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

.message-text {
  padding: 12px 16px;
  line-height: 1.7;
  font-size: 16px;
  word-wrap: break-word;
  display: inline-block;
}

.message.assistant .message-text {
  color: var(--text-primary);
  padding: 0;
  display: block;
  width: 100%;
  text-align: left;
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

.typing-indicator span:nth-child(1) {
  animation-delay: 0s;
}
.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}
.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
