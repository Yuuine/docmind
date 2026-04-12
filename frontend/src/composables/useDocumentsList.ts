import { ref } from 'vue'
import axios from 'axios'
import { documentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Document } from '@/types'
import { normalizeDocumentList } from '@/utils/documents'

export function useDocumentsList() {
  const userStore = useUserStore()
  const documents = ref<Document[]>([])
  const currentPage = ref(1)
  const pageSize = 10
  const total = ref(0)
  const totalPages = ref(0)
  const searchQuery = ref('')

  let listAbortController: AbortController | null = null

  async function loadDocuments() {
    const userId = userStore.user?.id
    if (!userId) return
    listAbortController?.abort()
    listAbortController = new AbortController()
    const signal = listAbortController.signal

    const params: { page?: number; pageSize?: number; filename?: string } = {
      page: currentPage.value,
      pageSize
    }
    if (searchQuery.value.trim()) {
      params.filename = searchQuery.value.trim()
    }
    try {
      const result = await documentApi.list(params, userId, signal)
      documents.value = normalizeDocumentList(result.records)
      total.value = result.total
      totalPages.value = result.totalPages
    } catch (e: unknown) {
      if (axios.isCancel(e)) return
      if (typeof e === 'object' && e !== null && (e as { code?: string }).code === 'ERR_CANCELED') return
      throw e
    }
  }

  function setPage(page: number) {
    currentPage.value = page
    loadDocuments()
  }

  function setSearchQuery(query: string) {
    searchQuery.value = query
    currentPage.value = 1
    loadDocuments()
  }

  return { 
    documents, 
    currentPage, 
    pageSize, 
    total, 
    totalPages, 
    searchQuery,
    loadDocuments,
    setPage,
    setSearchQuery
  }
}
