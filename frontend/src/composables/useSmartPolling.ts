import type { Ref } from 'vue'

interface UseSmartPollingOptions {
  fastInterval?: number
  slowInterval?: number
  hasProcessing: () => boolean
  onPoll: () => void | Promise<void>
}

export function useSmartPolling(options: UseSmartPollingOptions) {
  const {
    fastInterval = 2000,
    slowInterval = 60000,
    hasProcessing,
    onPoll
  } = options

  let pollingInterval: ReturnType<typeof setInterval> | null = null
  let visibilityChangeHandler: (() => void) | null = null

  function getPollingInterval(): number {
    return hasProcessing() ? fastInterval : slowInterval
  }

  function startPolling() {
    stopPolling()
    const interval = getPollingInterval()
    pollingInterval = setInterval(() => {
      if (!document.hidden) {
        onPoll()
      }
    }, interval)
  }

  function stopPolling() {
    if (pollingInterval) {
      clearInterval(pollingInterval)
      pollingInterval = null
    }
  }

  function restartPollingIfNeeded() {
    if (pollingInterval) {
      startPolling()
    }
  }

  function setupVisibilityListener() {
    visibilityChangeHandler = () => {
      if (document.hidden) {
        stopPolling()
      } else {
        onPoll()
        startPolling()
      }
    }
    document.addEventListener('visibilitychange', visibilityChangeHandler)
  }

  function cleanup() {
    stopPolling()
    if (visibilityChangeHandler) {
      document.removeEventListener('visibilitychange', visibilityChangeHandler)
      visibilityChangeHandler = null
    }
  }

  return {
    startPolling,
    stopPolling,
    restartPollingIfNeeded,
    setupVisibilityListener,
    cleanup
  }
}
