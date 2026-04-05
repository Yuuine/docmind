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

const checkPosition = () => {
  if (!props.container) return

  const { scrollTop, scrollHeight, clientHeight } = props.container
  const distanceFromBottom = scrollHeight - scrollTop - clientHeight
  visible.value = distanceFromBottom > props.threshold
}

const scrollToBottom = () => {
  if (!props.container) return

  props.container.scrollTo({
    top: props.container.scrollHeight,
    behavior: 'smooth'
  })
}

const handleClick = () => {
  scrollToBottom()
  emit('scrollToBottom')
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
  if (scrollHandler && props.container) {
    props.container.removeEventListener('scroll', scrollHandler)
  }
})

defineExpose({
  scrollToBottom,
  checkPosition
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
