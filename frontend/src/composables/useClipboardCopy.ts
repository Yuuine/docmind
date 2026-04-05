import { useToastStore } from '@/stores/toast'

export function useClipboardCopy() {
  const toastStore = useToastStore()

  async function copyMessage(content: string) {
    try {
      await navigator.clipboard.writeText(content)
      toastStore.success('已复制到剪贴板')
    } catch {
      toastStore.error('复制失败')
    }
  }

  return { copyMessage }
}
