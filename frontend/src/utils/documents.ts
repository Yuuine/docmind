import type { Document, PageResult } from '@/types'

function isPageResultOfDocuments(x: unknown): x is PageResult<Document> {
  if (typeof x !== 'object' || x === null || !('items' in x)) return false
  const items = (x as { items: unknown }).items
  return Array.isArray(items)
}

/** Accepts API array, paginated shape, or unknown — same fallbacks as legacy `items || result || []`. */
export function normalizeDocumentList(data: unknown): Document[] {
  if (Array.isArray(data)) return data as Document[]
  if (isPageResultOfDocuments(data)) return data.items
  return []
}
