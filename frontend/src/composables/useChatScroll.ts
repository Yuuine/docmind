import { ref, nextTick, watch, onBeforeUnmount, type Ref } from 'vue'
import type { ChatMessage } from '@/types'

interface UseChatScrollOptions {
  scrollEl: Ref<HTMLElement | undefined>
  isStreaming: () => boolean
  getMessages: () => ChatMessage[]
  getStreamingContent: () => string
}

/**
 * Keeps chat scroll position in sync with streaming and new messages (same thresholds as legacy ChatPage).
 */
export function useChatScroll(options: UseChatScrollOptions) {
  const isUserNearBottom = ref(true)
  const isComponentMounted = ref(true)
  let scrollBottomRaf = 0

  const stopMessagesWatch = watch(
    () => options.getMessages(),
    () => smartScrollToBottom(),
    { deep: true }
  )

  const stopStreamingWatch = watch(
    () => options.getStreamingContent(),
    () => smartScrollToBottom()
  )

  function onMessagesScroll() {
    const container = options.scrollEl.value
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
        const container = options.scrollEl.value
        if (!container) return
        const threshold = 150
        const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight

        if (options.isStreaming()) {
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

  /**
   * 强制滚到底（用于切换会话等）：先标记贴底，再在多帧后重复设置 scrollTop，
   * 以便虚拟列表完成测量后 scrollHeight 已更新。
   */
  function scrollToBottomImmediate() {
    isUserNearBottom.value = true
    const apply = () => {
      const container = options.scrollEl.value
      if (!container) return
      container.scrollTop = container.scrollHeight
    }
    nextTick(() => {
      apply()
      requestAnimationFrame(() => {
        apply()
        requestAnimationFrame(() => {
          apply()
          requestAnimationFrame(apply)
        })
      })
    })
  }

  /** Call before sending a message so streaming follows the bottom like legacy ChatPage. */
  function prepareScrollForOutgoingMessage() {
    isUserNearBottom.value = true
  }

  onBeforeUnmount(() => {
    isComponentMounted.value = false
    if (scrollBottomRaf) cancelAnimationFrame(scrollBottomRaf)
    scrollBottomRaf = 0
    stopMessagesWatch()
    stopStreamingWatch()
  })

  return {
    isUserNearBottom,
    onMessagesScroll,
    smartScrollToBottom,
    scrollToBottomImmediate,
    prepareScrollForOutgoingMessage
  }
}
