import { ref } from 'vue'
import { documentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Document } from '@/types'
import { normalizeDocumentList } from '@/utils/documents'

export function useDocumentsList() {
  const userStore = useUserStore()
  const documents = ref<Document[]>([])

  async function loadDocuments() {
    const userId = userStore.user?.id
    if (!userId) return
    const result = await documentApi.list({ page: 1, size: 100 })
    documents.value = normalizeDocumentList(result)
  }

  return { documents, loadDocuments }
}
