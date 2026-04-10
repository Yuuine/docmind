import { ref, watch, type Ref } from 'vue'

interface UseDebouncedSearchOptions {
  delay?: number
  onSearch: (query: string) => void
}

export function useDebouncedSearch(options: UseDebouncedSearchOptions) {
  const { delay = 300, onSearch } = options

  const localSearchQuery = ref('')
  let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

  watch(localSearchQuery, (newValue) => {
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer)
    }
    searchDebounceTimer = setTimeout(() => {
      onSearch(newValue)
    }, delay)
  })

  function cleanup() {
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer)
      searchDebounceTimer = null
    }
  }

  return {
    localSearchQuery,
    cleanup
  }
}
