import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
  id: string
  message: string
  type: ToastType
  duration: number
  shake?: boolean
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])
  let idCounter = 0

  function add(message: string, type: ToastType = 'info', duration: number = 3000, shake: boolean = false) {
    const id = `toast-${++idCounter}`
    const toast: Toast = {
      id,
      message,
      type,
      duration,
      shake
    }
    toasts.value.push(toast)

    if (duration > 0) {
      setTimeout(() => {
        remove(id)
      }, duration)
    }

    return id
  }

  function remove(id: string) {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index > -1) {
      toasts.value.splice(index, 1)
    }
  }

  function success(message: string, duration?: number) {
    return add(message, 'success', duration)
  }

  function error(message: string, duration?: number, shake: boolean = true) {
    return add(message, 'error', duration, shake)
  }

  function warning(message: string, duration?: number) {
    return add(message, 'warning', duration)
  }

  function info(message: string, duration?: number) {
    return add(message, 'info', duration)
  }

  function clear() {
    toasts.value = []
  }

  return {
    toasts,
    add,
    remove,
    success,
    error,
    warning,
    info,
    clear
  }
})
