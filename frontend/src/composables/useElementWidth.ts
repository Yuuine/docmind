import { ref, watch, onUnmounted, type Ref } from 'vue'

/**
 * 订阅元素内容宽度（clientWidth），用于 Pretext layout 的 maxWidth，避免 getBoundingClientRect 回流链。
 */
export function useElementWidth(elRef: Ref<HTMLElement | null>, min = 160, fallback = 560) {
  const width = ref(fallback)
  let ro: ResizeObserver | null = null
  let widthRaf = 0

  function setFromEl(el: HTMLElement) {
    if (widthRaf) cancelAnimationFrame(widthRaf)
    widthRaf = requestAnimationFrame(() => {
      widthRaf = 0
      width.value = Math.max(min, Math.floor(el.clientWidth))
    })
  }

  watch(
    elRef,
    el => {
      ro?.disconnect()
      ro = null
      if (widthRaf) {
        cancelAnimationFrame(widthRaf)
        widthRaf = 0
      }
      if (!el) {
        width.value = fallback
        return
      }
      setFromEl(el)
      ro = new ResizeObserver(() => setFromEl(el))
      ro.observe(el)
    },
    { immediate: true }
  )

  onUnmounted(() => {
    if (widthRaf) cancelAnimationFrame(widthRaf)
    widthRaf = 0
    ro?.disconnect()
    ro = null
  })

  return width
}
