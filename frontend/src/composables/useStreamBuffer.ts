import { ref, onBeforeUnmount, type Ref } from 'vue'

interface UseStreamBufferOptions {
  onContentUpdate: (content: string) => void
  onHeightUpdate?: (height: number) => void
  defaultBatchSize?: number
  middleBatchSize?: number
  maxBatchSize?: number
  lowThreshold?: number
  highThreshold?: number
}

interface UseStreamBufferReturn {
  pushText: (text: string) => void
  clear: () => void
  isRendering: Ref<boolean>
  bufferLength: Ref<number>
  bufferSize: Ref<number>
}

export function useStreamBuffer(options: UseStreamBufferOptions): UseStreamBufferReturn {
  const {
    onContentUpdate,
    onHeightUpdate,
    defaultBatchSize = 3,
    middleBatchSize = 10,
    maxBatchSize = 20,
    lowThreshold = 500,
    highThreshold = 1000
  } = options

  const buffer: string[] = []
  let renderedContent = ''
  let rafId = 0
  let heightTimer: ReturnType<typeof setTimeout> | null = null
  let hasInitialRender = false

  const isRendering = ref(false)
  const bufferLength = ref(0)
  const bufferSize = ref(0)

  function flushHeightUpdate(content: string) {
    if (!onHeightUpdate) return
    if (heightTimer) clearTimeout(heightTimer)
    heightTimer = setTimeout(() => {
      heightTimer = null
      onHeightUpdate(content.length)
    }, 120)
  }

  function calculateDynamicBatch(): number {
    const len = buffer.length
    if (len > highThreshold) {
      return maxBatchSize
    } else if (len > lowThreshold) {
      return middleBatchSize
    } else {
      return defaultBatchSize
    }
  }

  function consumeBatch(): string[] {
    const batchSize = calculateDynamicBatch()
    const batch = buffer.splice(0, batchSize)
    bufferLength.value = buffer.length
    return batch
  }

  function renderLoop() {
    if (buffer.length === 0) {
      isRendering.value = false
      rafId = 0
      return
    }

    const batch = consumeBatch()
    renderedContent += batch.join('')
    bufferSize.value = renderedContent.length

    onContentUpdate(renderedContent)
    flushHeightUpdate(renderedContent)

    rafId = requestAnimationFrame(renderLoop)
  }

  function ensureLoopRunning() {
    if (rafId !== 0) return
    isRendering.value = true
    rafId = requestAnimationFrame(renderLoop)
  }

  function pushText(text: string) {
    if (!text) return
    const chars = text.split('')
    buffer.push(...chars)
    bufferLength.value = buffer.length
    
    if (!hasInitialRender && renderedContent.length === 0) {
      hasInitialRender = true
      const initialContent = chars.join('')
      renderedContent = initialContent
      bufferSize.value = renderedContent.length
      
      buffer.splice(0, chars.length)
      bufferLength.value = buffer.length
      
      onContentUpdate(renderedContent)
      flushHeightUpdate(renderedContent)
    }
    
    ensureLoopRunning()
  }

  function clear() {
    if (rafId !== 0) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
    if (heightTimer) {
      clearTimeout(heightTimer)
      heightTimer = null
    }
    buffer.length = 0
    renderedContent = ''
    isRendering.value = false
    bufferLength.value = 0
    bufferSize.value = 0
    hasInitialRender = false
  }

  onBeforeUnmount(() => {
    if (rafId !== 0) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
    if (heightTimer) {
      clearTimeout(heightTimer)
      heightTimer = null
    }
  })

  return { pushText, clear, isRendering, bufferLength, bufferSize }
}