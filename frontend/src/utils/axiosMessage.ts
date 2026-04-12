import axios from 'axios'

export function getAxiosErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const msg = error.response?.data as { message?: string } | undefined
    if (msg && typeof msg.message === 'string') return msg.message
  }
  return fallback
}
