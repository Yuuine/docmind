/** Minimal toast API used by send-message error mapping. */
export interface ToastErrorApi {
  error: (message: string, duration?: number, shake?: boolean) => void
}

/** Fetch stream errors carry HTTP status like the previous mutable Error + status pattern. */
export function getErrorWithStatus(error: unknown): { status?: number; message: string } {
  if (typeof error !== 'object' || error === null) {
    return { message: '发送失败' }
  }
  const o = error as Record<string, unknown>
  const status = typeof o.status === 'number' ? o.status : undefined
  const message =
    typeof o.message === 'string' ? o.message : error instanceof Error ? error.message : '发送失败'
  return { status, message }
}

/**
 * Maps send/stream failures to the same toast messages as the legacy ChatPage catch block.
 */
export function showSendMessageErrorToast(toast: ToastErrorApi, error: unknown): void {
  const { status, message } = getErrorWithStatus(error)

  if (status != null) {
    if (status === 401 || status === 403) {
      toast.error('登录已过期，请重新登录')
      return
    }
    if (status === 404) {
      toast.error('会话不存在，请刷新页面重试')
      return
    }
    if (status >= 500) {
      toast.error('服务器内部错误，请稍后重试')
      return
    }
    toast.error(message || '请求失败')
    return
  }

  const errMsg = error instanceof Error ? error.message : '发送失败'
  if (errMsg.includes('missing userId') || errMsg.includes('missing sessionId')) {
    toast.error('会话状态异常，请刷新页面')
    return
  }
  if (
    errMsg.includes('fetch') ||
    errMsg.includes('network') ||
    errMsg.includes('Failed to fetch')
  ) {
    toast.error('网络连接失败，请检查网络')
    return
  }
  toast.error(errMsg)
}
