<template>
  <Transition name="fade">
    <button
      v-if="visible"
      type="button"
      class="scroll-to-bottom-btn"
      @click="handleClick"
      title="滚动到底部"
    >
      <Icon name="arrowDown" :size="16" />
      <span class="scroll-to-bottom-label">回到底部</span>
    </button>
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { Icon } from './icons'

interface Props {
  container: HTMLElement | null
  threshold?: number
}

const props = withDefaults(defineProps<Props>(), {
  threshold: 120
})

const emit = defineEmits<{
  scrollToBottom: []
}>()

const visible = ref(false)
let scrollHandler: (() => void) | null = null
let isScrollingToBottom = false
let animationFrameId = 0

const checkPosition = () => {
  if (!props.container) return

  const { scrollTop, scrollHeight, clientHeight } = props.container
  const distanceFromBottom = scrollHeight - scrollTop - clientHeight
  visible.value = distanceFromBottom > props.threshold
}

/**
 * 缓动函数：easeOutQuart
 * 特点：开始速度快，逐渐减速，结束时速度为0
 * 效果：初始有爆发力，平滑停止无跳动
 */
function easeOutQuart(t: number): number {
  return 1 - Math.pow(1 - t, 4)
}

/**
 * 缓动函数：easeInOutCubic
 * 特点：前半段加速，后半段减速
 * 效果：非常自然的变速运动
 */
function easeInOutCubic(t: number): number {
  return t < 0.5 
    ? 4 * t * t * t 
    : 1 - Math.pow(-2 * t + 2, 3) / 2
}

/**
 * 自定义变速滚动动画
 * 
 * 特点：
 * 1. 使用 requestAnimationFrame 实现 60fps 流畅动画
 * 2. 缓动函数控制速度变化（开始快 → 结束慢）
 * 3. 每帧动态读取 scrollHeight，避免虚拟列表导致的过冲
 * 4. 精确的终止条件，无回弹
 */
function animatedScrollToBottom(
  duration: number = 500,
  easingFn: (t: number) => number = easeOutQuart
) {
  if (!props.container || isScrollingToBottom) return
  
  isScrollingToBottom = true
  
  const container = props.container
  let startTime: number | null = null
  let startScrollTop = container.scrollTop
  
  const animate = (currentTime: number) => {
    if (!props.container) {
      isScrollingToBottom = false
      return
    }
    
    if (startTime === null) {
      startTime = currentTime
    }
    
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    
    const easedProgress = easingFn(progress)
    
    const targetScrollTop = props.container.scrollHeight - props.container.clientHeight
    const distance = targetScrollTop - startScrollTop
    
    const currentScrollTop = startScrollTop + (distance * easedProgress)
    
    props.container.scrollTop = currentScrollTop
    
    if (progress < 1) {
      animationFrameId = requestAnimationFrame(animate)
    } else {
      props.container.scrollTop = targetScrollTop
      
      isScrollingToBottom = false
      
      checkPosition()
      
      emit('scrollToBottom')
    }
  }
  
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  
  animationFrameId = requestAnimationFrame(animate)
}

const forceScrollToBottomInstant = () => {
  if (!props.container) return
  
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = 0
  }
  
  isScrollingToBottom = false
  
  props.container.scrollTop = props.container.scrollHeight - props.container.clientHeight
  
  checkPosition()
}

const scrollToBottom = () => {
  if (!props.container || isScrollingToBottom) return
  
  animatedScrollToBottom(450, easeOutQuart)
}

const handleClick = () => {
  scrollToBottom()
}

watch(() => props.container, (newContainer) => {
  if (scrollHandler) {
    props.container?.removeEventListener('scroll', scrollHandler)
  }

  if (newContainer) {
    scrollHandler = () => checkPosition()
    newContainer.addEventListener('scroll', scrollHandler, { passive: true })
    checkPosition()
  }
}, { immediate: true })

onMounted(() => {
  checkPosition()
})

onBeforeUnmount(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = 0
  }
  if (scrollHandler && props.container) {
    props.container.removeEventListener('scroll', scrollHandler)
  }
})

defineExpose({
  scrollToBottom,
  checkPosition,
  animatedScrollToBottom,
  forceScrollToBottomInstant
})
</script>

<style scoped>
.scroll-to-bottom-btn {
  position: relative;
  z-index: 110;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--border-light);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  line-height: 1.2;
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  -webkit-tap-highlight-color: transparent;
}

.scroll-to-bottom-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border-color: var(--border-light);
  box-shadow: var(--shadow-lg);
}

.scroll-to-bottom-btn:active {
  filter: brightness(0.97);
}

.scroll-to-bottom-btn :deep(svg) {
  display: block;
  flex-shrink: 0;
}

.scroll-to-bottom-label {
  white-space: nowrap;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
