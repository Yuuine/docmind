/** Thrown when SSE stream HTTP response is not OK; mirrors previous Error + numeric status fields. */
export class StreamHttpError extends Error {
  readonly status: number
  readonly statusText: string

  constructor(status: number, statusText: string) {
    super(`Stream request failed: ${status}`)
    this.name = 'StreamHttpError'
    this.status = status
    this.statusText = statusText
  }
}
