import { computed, type Ref } from 'vue'
import type { ChatSession } from '@/types'

export interface SessionGroup {
  label: string
  sessions: ChatSession[]
}

/** Buckets sessions by last activity: within 7d, 7d–30d, older. */
export function useGroupedSessions(sessions: Ref<ChatSession[]>) {
  return computed<SessionGroup[]>(() => {
    const now = new Date()
    const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
    const oneMonthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)

    const withinWeek: ChatSession[] = []
    const withinMonth: ChatSession[] = []
    const beforeMonth: ChatSession[] = []

    const sorted = [...sessions.value].sort(
      (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    )

    for (const session of sorted) {
      const updatedAt = new Date(session.updatedAt)
      if (updatedAt >= oneWeekAgo) {
        withinWeek.push(session)
      } else if (updatedAt >= oneMonthAgo) {
        withinMonth.push(session)
      } else {
        beforeMonth.push(session)
      }
    }

    return [
      { label: '一周内', sessions: withinWeek },
      { label: '一周前', sessions: withinMonth },
      { label: '一月前', sessions: beforeMonth }
    ]
  })
}
