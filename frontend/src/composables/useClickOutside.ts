import { onMounted, onBeforeUnmount, type Ref } from 'vue'

export function useClickOutside(
  targetRef: Ref<HTMLElement | undefined>,
  onOutside: (event: MouseEvent) => void
) {
  function handleClick(event: MouseEvent) {
    if (targetRef.value && !targetRef.value.contains(event.target as Node)) {
      onOutside(event)
    }
  }

  onMounted(() => document.addEventListener('click', handleClick))
  onBeforeUnmount(() => document.removeEventListener('click', handleClick))
}
