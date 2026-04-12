import type { Document, PageResponse, PageResult } from '@/types'

function isPageResponseOfDocuments(x: unknown): x is PageResponse<Document> {
  if (typeof x !== 'object' || x === null) return false
  const obj = x as Record<string, unknown>
  return 'records' in obj && Array.isArray(obj.records)
}

function isPageResultOfDocuments(x: unknown): x is PageResult<Document> {
  if (typeof x !== 'object' || x === null || !('items' in x)) return false
  return Array.isArray((x as { items: unknown }).items)
}

export function normalizeDocumentList(data: unknown): Document[] {
  if (Array.isArray(data)) return data as Document[]
  if (isPageResponseOfDocuments(data)) return data.records
  if (isPageResultOfDocuments(data)) return data.items
  return []
}
