<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">分块详情</h3>
        <button class="modal-close" @click="$emit('close')">
          <Icon name="close" :size="20" />
        </button>
      </div>
      <div class="modal-body">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p class="loading-text">加载中...</p>
        </div>
        <div v-else-if="chunk" class="chunk-detail">
          <div class="chunk-meta">
            <div class="meta-item">
              <span class="meta-label">分块索引</span>
              <span class="meta-value">Chunk {{ chunk.chunkIndex }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">字符数</span>
              <span class="meta-value">{{ chunk.charCount.toLocaleString() }} 字</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">创建时间</span>
              <span class="meta-value">{{ formatDate(chunk.createdAt) }}</span>
            </div>
          </div>
          <div class="chunk-content-section">
            <div class="content-header">
              <h4 class="content-title">内容</h4>
              <button class="copy-btn" @click="copyContent">
                <Icon name="copy" :size="14" />
                <span>{{ copied ? '已复制' : '复制' }}</span>
              </button>
            </div>
            <div class="chunk-content">{{ chunk.content }}</div>
          </div>
        </div>
        <div v-else class="error-container">
          <Icon name="alert-circle" :size="48" />
          <p class="error-text">加载失败</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { documentApi } from '@/api'
import { Icon } from '@/components/icons'
import { useToastStore } from '@/stores/toast'
import type { DocumentChunkInfo } from '@/types'

const props = defineProps<{
  chunkId: number
  userId?: number
}>()

defineEmits<{
  close: []
}>()

const toastStore = useToastStore()

const chunk = ref<DocumentChunkInfo | null>(null)
const loading = ref(false)
const copied = ref(false)

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function loadChunk() {
  if (!props.userId) return
  loading.value = true
  try {
    chunk.value = await documentApi.getChunk(props.chunkId, props.userId)
  } catch (error) {
    console.error('Failed to load chunk:', error)
  } finally {
    loading.value = false
  }
}

async function copyContent() {
  if (!chunk.value?.content) return
  try {
    await navigator.clipboard.writeText(chunk.value.content)
    copied.value = true
    toastStore.success('已复制到剪贴板')
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (error) {
    console.error('Failed to copy:', error)
    toastStore.error('复制失败')
  }
}

watch(() => props.chunkId, () => {
  chunk.value = null
  loadChunk()
}, { immediate: true })
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.15s ease;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 800px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: scaleIn 0.15s ease;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--glass-border);
  flex-shrink: 0;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  gap: 12px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: var(--btn-primary-bg);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  gap: 12px;
  color: #991b1b;
}

.error-text {
  font-size: 14px;
  margin: 0;
}

.chunk-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chunk-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 16px;
  background: var(--glass-bg-light);
  border: 1px solid var(--glass-border-subtle);
  border-radius: 12px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: #888;
}

.meta-value {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
}

.chunk-content-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.content-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin: 0;
}

.copy-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--glass-bg-light);
  border: 1px solid var(--glass-border-subtle);
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
}

.copy-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: var(--glass-border);
}

.chunk-content {
  padding: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: #1f2937;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 400px;
  overflow-y: auto;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
