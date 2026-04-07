import { ref, nextTick, watch, onBeforeUnmount, type Ref } from 'vue'
import type { ChatMessage } from '@/types'
import { measureMarkdownRoughHeight } from '@/utils/pretextLayout'
import { useElementWidth } from '@/composables/useElementWidth'

interface UseChatScrollOptions {
  scrollEl: Ref<HTMLElement | undefined>
  isStreaming: () => boolean
  getMessages: () => ChatMessage[]
  getStreamingContent: () => string
}

/**
 * 聊天页面滚动管理器
 * 
 * 核心策略：
 * - 发送消息：先预计算消息高度，再平滑滚动到正确位置（和"回到底部"按钮一致）
 * - 流式输出：rAF合并 + 即时滚动（保持响应速度）
 * - 切换会话：多帧确认滚动（确保位置准确）
 */
export function useChatScroll(options: UseChatScrollOptions) {
  const isUserNearBottom = ref(true)
  const isComponentMounted = ref(true)
  
  let scrollRafId = 0
  let streamScrollTimer: ReturnType<typeof setTimeout> | null = null
  let afterSendTimer: ReturnType<typeof setTimeout> | null = null
  let isSendingMessage = false

  const stopMessagesWatch = watch(
    () => options.getMessages().length,
    () => {
      isUserNearBottom.value = true
      
      if (isSendingMessage) {
        return
      }
      
      scheduleInstantScroll()
    }
  )

  const stopStreamingWatch = watch(
    () => options.getStreamingContent(),
    (content) => {
      if (!isUserNearBottom.value) return
      if (streamScrollTimer) clearTimeout(streamScrollTimer)
      
      const hasNewLine = content.includes('\n')
      const delay = hasNewLine ? 0 : 50
      
      streamScrollTimer = setTimeout(() => {
        scheduleInstantScroll()
      }, delay)
    }
  )

  function onMessagesScroll() {
    const container = options.scrollEl.value
    if (!container) return
    const threshold = 150
    const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight
    isUserNearBottom.value = distanceFromBottom < threshold
  }

  /**
   * 即时滚动（用于流式输出等高频场景）
   * 通过rAF合并，每帧最多执行一次
   */
  function scheduleInstantScroll() {
    if (scrollRafId) return
    scrollRafId = requestAnimationFrame(() => {
      scrollRafId = 0
      if (!isComponentMounted.value) return
      instantScrollToBottom()
    })
  }

  function instantScrollToBottom(): void {
    const container = options.scrollEl.value
    if (!container) return
    container.scrollTo({ 
      top: container.scrollHeight, 
      behavior: 'instant' 
    })
  }

  function easeOutQuart(t: number): number {
    return 1 - Math.pow(1 - t, 4)
  }

  let smoothScrollRafId = 0

  function smoothScrollToBottom(): void {
    const container = options.scrollEl.value
    if (!container) return
    
    if (smoothScrollRafId) {
      cancelAnimationFrame(smoothScrollRafId)
    }
    
    const duration = 450
    const startTime = performance.now()
    const startScrollTop = container.scrollTop
    
    const animate = (currentTime: number) => {
      if (!options.scrollEl.value) return
      
      const elapsed = currentTime - startTime
      const progress = Math.min(elapsed / duration, 1)
      
      const easedProgress = easeOutQuart(progress)
      
      const targetScrollTop = options.scrollEl.value.scrollHeight - options.scrollEl.value.clientHeight
      const distance = targetScrollTop - startScrollTop
      
      options.scrollEl.value.scrollTop = startScrollTop + (distance * easedProgress)
      
      if (progress < 1) {
        smoothScrollRafId = requestAnimationFrame(animate)
      } else {
        smoothScrollRafId = 0
        options.scrollEl.value.scrollTop = targetScrollTop
      }
    }
    
    smoothScrollRafId = requestAnimationFrame(animate)
  }

  /**
   * 预估用户消息的像素高度
   * 用于在DOM更新前计算滚动偏移量
   */
  function estimateUserMessageHeight(content: string): number {
    const container = options.scrollEl.value
    if (!container || !content) return 80
    
    const containerWidth = container.clientWidth - 80
    if (containerWidth <= 0) return 80
    
    const estimatedHeight = measureMarkdownRoughHeight(content, containerWidth)
    
    const baseHeight = Math.max(52, Math.ceil(estimatedHeight))
    const marginBottom = 28
    
    return baseHeight + marginBottom
  }

  /**
   * 发送消息后的智能滚动
   * 
   * 策略：
   * 1. 先记录当前状态
   * 2. 等待DOM更新（nextTick）
   * 3. 执行平滑滚动到正确位置（和"回到底部"按钮行为一致）
   * 4. 延迟确认，修正可能的偏差
   */
  async function scrollAfterMessageSent(userContent?: string): Promise<void> {
    isUserNearBottom.value = true
    isSendingMessage = true
    
    if (afterSendTimer) {
      clearTimeout(afterSendTimer)
    }
    
    await nextTick()
    
    smoothScrollToBottom()
    
    afterSendTimer = setTimeout(() => {
      afterSendTimer = null
      isSendingMessage = false
      
      smoothScrollToBottom()
      
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          smoothScrollToBottom()
        })
      })
    }, 100)
    
    await new Promise(resolve => setTimeout(resolve, 180))
  }

  /**
   * 强制滚到底（用于切换会话等场景）
   * 多帧确认，确保虚拟列表测量完成后位置正确
   */
  function scrollToBottomImmediate() {
    isUserNearBottom.value = true
    const apply = () => {
      const container = options.scrollEl.value
      if (!container) return
      container.scrollTo({ top: container.scrollHeight, behavior: 'instant' })
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
    if (scrollRafId) cancelAnimationFrame(scrollRafId)
    if (smoothScrollRafId) cancelAnimationFrame(smoothScrollRafId)
    if (streamScrollTimer) clearTimeout(streamScrollTimer)
    if (afterSendTimer) clearTimeout(afterSendTimer)
    scrollRafId = 0
    smoothScrollRafId = 0
    stopMessagesWatch()
    stopStreamingWatch()
  })

  return {
    isUserNearBottom,
    onMessagesScroll,
    smartScrollToBottom: scheduleInstantScroll,
    scrollToBottomImmediate,
    prepareScrollForOutgoingMessage,
    scrollAfterMessageSent,
    forceScrollToBottom: instantScrollToBottom,
    estimateUserMessageHeight
  }
}
