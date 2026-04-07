<template>
  <div
    class="virtual-list-inner"
    :style="{
      height: `${rowVirtualizer.getTotalSize()}px`,
      width: '100%',
      position: 'relative'
    }"
  >
    <div
      v-for="virtualRow in rowVirtualizer.getVirtualItems()"
      :key="String(virtualRow.key)"
      :data-index="virtualRow.index"
      :ref="(el) => bindMeasureRef(el)"
      class="virtual-row"
      :style="{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        transform: `translateY(${virtualRow.start}px)`
      }"
    >
      <div :class="['message', messages[virtualRow.index]?.role.toLowerCase() ?? 'user']">
        <div
          class="message-content"
          :class="{
            'message-content--assistant': messages[virtualRow.index]?.role === 'ASSISTANT'
          }"
        >
          <template v-if="messages[virtualRow.index]?.role === 'ASSISTANT'">
            <AssistantMessageCard>
              <MarkdownRenderer
                :content="messages[virtualRow.index]?.content ?? ''"
                :is-streaming="
                  chatStore.isStreaming && virtualRow.index === lastAssistantMessageIndex
                "
              />
              <template v-if="!(chatStore.isStreaming && virtualRow.index === lastAssistantMessageIndex)" #footer>
                <div class="message-meta-row message-meta-row--assistant">
                  <span class="message-time">{{
                    formatTime(messages[virtualRow.index]?.createdAt ?? '')
                  }}</span>
                  <button
                    type="button"
                    class="action-copy-btn"
                    @click="copyMessage(messages[virtualRow.index]?.content ?? '')"
                  >
                    <Icon name="copy" :size="14" />
                  </button>
                </div>
              </template>
            </AssistantMessageCard>
          </template>
          <template v-else>
            <div class="message-user-stack">
              <div class="message-text">
                {{ messages[virtualRow.index]?.content }}
              </div>
              <div class="message-meta-row message-meta-row--user">
                <button
                  type="button"
                  class="action-copy-btn"
                  @click="copyMessage(messages[virtualRow.index]?.content ?? '')"
                >
                  <Icon name="copy" :size="14" />
                </button>
                <span class="message-time">{{
                  formatTime(messages[virtualRow.index]?.createdAt ?? '')
                }}</span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, nextTick, type ComponentPublicInstance } from 'vue'
import { storeToRefs } from 'pinia'
import { useVirtualizer, measureElement } from '@tanstack/vue-virtual'
import { useChatStore } from '@/stores/chat'
import { useClipboardCopy } from '@/composables/useClipboardCopy'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AssistantMessageCard from '@/components/AssistantMessageCard.vue'
import { Icon } from '@/components/icons'

const props = defineProps<{
  getScrollParent: () => HTMLElement | undefined | null
}>()

const chatStore = useChatStore()
const { messages } = storeToRefs(chatStore)
const { copyMessage } = useClipboardCopy()

const lastAssistantMessageIndex = computed(() => {
  const m = messages.value
  const len = m.length
  if (len === 0) return -1
  const last = len - 1
  return m[last]?.role === 'ASSISTANT' ? last : -1
})

let lastMessagesLength = 0

const virtualizerOptions = computed(() => ({
  count: messages.value.length,
  getScrollElement: () => props.getScrollParent() ?? null,
  estimateSize: () => 120,
  overscan: 8,
  getItemKey: (index: number) => messages.value[index]?.id ?? index,
  measureElement
}))

const rowVirtualizer = useVirtualizer(virtualizerOptions)

function bindMeasureRef(el: Element | ComponentPublicInstance | null) {
  rowVirtualizer.value.measureElement(el as HTMLElement | null)
}

watch(
  () => messages.value.length,
  (newLength) => {
    if (newLength !== lastMessagesLength) {
      lastMessagesLength = newLength
      nextTick(() => rowVirtualizer.value.measure())
    }
  }
)

function formatTime(dateString: string) {
  const date = new Date(dateString)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
/* 间距纳入测量，避免 margin 塌缩导致虚拟高度偏小、行间重叠 */
.virtual-row {
  box-sizing: border-box;
  padding-bottom: 28px;
}

.virtual-row .message {
  margin-bottom: 0;
}

.virtual-list-inner {
  width: 100%;
  position: relative;
}

.message {
  display: flex;
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
</style>
