import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { modelApi } from '@/api'
import type { AIModel, AIModelCreateRequest, AIModelUpdateRequest } from '@/types'

export const useModelsStore = defineStore('models', () => {
  const models = ref<AIModel[]>([])
  const isLoading = ref(false)

  const activeModel = computed(() => {
    return models.value.find(m => m.isActive === true) || null
  })

  const loadModels = async (userId: number) => {
    isLoading.value = true
    try {
      const data = await modelApi.getModels(userId)
      models.value = data || []
    } catch (error) {
      console.error('Load models failed:', error)
    } finally {
      isLoading.value = false
    }
  }

  const createModel = async (data: AIModelCreateRequest, userId?: number) => {
    if (!userId) return
    isLoading.value = true
    try {
      await modelApi.createModel(data, userId)
      await loadModels(userId)
    } catch (error) {
      console.error('Create model failed:', error)
    } finally {
      isLoading.value = false
    }
  }

  const updateModel = async (id: number, data: AIModelUpdateRequest, userId?: number) => {
    if (!userId) return
    isLoading.value = true
    try {
      await modelApi.updateModel(id, data, userId)
      await loadModels(userId)
    } catch (error) {
      console.error('Update model failed:', error)
    } finally {
      isLoading.value = false
    }
  }

  const deleteModel = async (id: number, userId?: number) => {
    if (!userId) return
    isLoading.value = true
    try {
      await modelApi.deleteModel(id, userId)
      await loadModels(userId)
    } catch (error) {
      console.error('Delete model failed:', error)
    } finally {
      isLoading.value = false
    }
  }

  const activateModel = async (id: number, userId?: number) => {
    if (!userId) return
    isLoading.value = true
    try {
      await modelApi.activateModel(id, userId)
      await loadModels(userId)
    } catch (error) {
      console.error('Activate model failed:', error)
    } finally {
      isLoading.value = false
    }
  }

  return {
    models,
    isLoading,
    activeModel,
    loadModels,
    createModel,
    updateModel,
    deleteModel,
    activateModel
  }
})
