import { ref } from 'vue'
import { documentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Document } from '@/types'
import { normalizeDocumentList } from '@/utils/documents'

export function useDocumentsList() {
  const userStore = useUserStore()
  const documents = ref<Document[]>([])
  const currentPage = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const totalPages = ref(0)
  const searchQuery = ref('')

  async function loadDocuments() {
    const userId = userStore.user?.id
    if (!userId) return
    const params: { page?: number; pageSize?: number; filename?: string } = { 
      page: currentPage.value, 
      pageSize: pageSize.value 
    }
    if (searchQuery.value.trim()) {
      params.filename = searchQuery.value.trim()
    }
    console.log('loadDocuments called with params:', params)
    const result = await documentApi.list(params, userId)
    console.log('API response:', result)
    documents.value = normalizeDocumentList(result.records)
    total.value = result.total
    totalPages.value = result.totalPages
    console.log('documents:', documents.value.length, 'total:', total.value, 'totalPages:', totalPages.value)
  }

  function setPage(page: number) {
    currentPage.value = page
    loadDocuments()
  }

  function setPageSize(size: number) {
    pageSize.value = size
    currentPage.value = 1
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
    setPageSize,
    setSearchQuery
  }
}
