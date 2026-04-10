<template>
  <div class="detail-panel" v-if="document">
    <div class="detail-section">
      <h4 class="section-title">基本信息</h4>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">文件大小</span>
          <span class="info-value">{{ formatFileSize(document.fileSize) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">文件类型</span>
          <span class="info-value">{{ document.contentType || '未知' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间</span>
          <span class="info-value">{{ formatDate(document.createdAt) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">更新时间</span>
          <span class="info-value">{{ formatDate(document.updatedAt) }}</span>
        </div>
      </div>
    </div>

    <div class="detail-section">
      <h4 class="section-title">处理状态</h4>
      <DocumentStatusTimeline :status="document.status" :error-message="document.errorMessage" />
      <div v-if="document.errorMessage" class="error-section">
        <button class="toggle-error-btn" @click="showError = !showError">
          <Icon :name="showError ? 'chevronDown' : 'chevronRight'" :size="14" />
          <span>查看错误详情</span>
        </button>
        <div v-if="showError" class="error-message">
          {{ document.errorMessage }}
        </div>
      </div>
    </div>

    <div class="detail-section">
      <h4 class="section-title">向量索引信息</h4>
      <div v-if="statsLoading" class="loading-skeleton">
        <div class="skeleton-item"></div>
        <div class="skeleton-item"></div>
        <div class="skeleton-item"></div>
      </div>
      <div v-else-if="stats" class="info-grid">
        <div class="info-item">
          <span class="info-label">总分块数</span>
          <span class="info-value">{{ stats.chunkCount }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">总字符数</span>
          <span class="info-value">{{ stats.totalCharCount.toLocaleString() }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">平均每块</span>
          <span class="info-value">{{ stats.avgChunkSize.toLocaleString() }} 字</span>
        </div>
      </div>
    </div>

    <div class="detail-section">
      <button class="toggle-chunks-btn" @click="toggleChunks">
        {{ showChunks ? '收起分块列表' : '显示分块列表' }}
      </button>
      <transition name="fade">
        <div v-if="showChunks" class="chunks-list">
          <div v-if="chunksLoading" class="loading-skeleton">
            <div class="skeleton-item" v-for="i in 3" :key="i"></div>
          </div>
          <div v-else-if="chunks" class="chunks-container">
            <div
              v-for="chunk in chunks"
              :key="chunk.id"
              class="chunk-item"
              @click="showChunkDetail(chunk.id)"
            >
              <div class="chunk-header">
                <span class="chunk-index">Chunk {{ chunk.chunkIndex }}</span>
                <span class="chunk-size">{{ chunk.charCount.toLocaleString() }} 字</span>
              </div>
              <div class="chunk-preview">{{ chunk.contentPreview }}</div>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <DocumentChunkModal
      v-if="showChunkModal"
      :chunk-id="selectedChunkId"
      :user-id="userId"
      @close="showChunkModal = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { documentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { Icon } from '@/components/icons'
import DocumentChunkModal from './DocumentChunkModal.vue'
import DocumentStatusTimeline from './DocumentStatusTimeline.vue'
import type { Document, DocumentStats, DocumentChunkInfo } from '@/types'

const props = defineProps<{
  document: Document
}>()

const userStore = useUserStore()
const userId = userStore.user?.id

const stats = ref<DocumentStats | null>(null)
const statsLoading = ref(false)
const chunks = ref<DocumentChunkInfo[] | null>(null)
const chunksLoading = ref(false)
const showChunks = ref(false)
const showError = ref(false)
const showChunkModal = ref(false)
const selectedChunkId = ref<number | null>(null)

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}



async function loadStats() {
  if (!userId) return
  statsLoading.value = true
  try {
    stats.value = await documentApi.getStats(props.document.id, userId)
  } catch (error) {
    console.error('Failed to load stats:', error)
  } finally {
    statsLoading.value = false
  }
}

async function loadChunks() {
  if (!userId) return
  chunksLoading.value = true
  try {
    chunks.value = await documentApi.getChunks(props.document.id, userId)
  } catch (error) {
    console.error('Failed to load chunks:', error)
  } finally {
    chunksLoading.value = false
  }
}

function toggleChunks() {
  showChunks.value = !showChunks.value
  if (showChunks.value && !chunks.value) {
    loadChunks()
  }
}

function showChunkDetail(chunkId: number) {
  selectedChunkId.value = chunkId
  showChunkModal.value = true
}

watch(() => props.document.id, () => {
  stats.value = null
  chunks.value = null
  showChunks.value = false
  showError.value = false
  nextTick(() => {
    loadStats()
  })
}, { immediate: true })
</script>

<style scoped>
.detail-panel {
  margin-top: 0;
  padding: 18px 18px 4px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(var(--blur-sm));
  -webkit-backdrop-filter: blur(var(--blur-sm));
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-border-subtle);
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section:last-of-type {
  display: flex;
  flex-direction: column;
}

.detail-section:last-of-type .section-title {
  align-self: flex-start;
}

.detail-section:last-of-type .toggle-chunks-btn {
  align-self: center;
}

.detail-section:last-of-type .chunks-list {
  width: 100%;
  align-self: stretch;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin: 0 0 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #888;
}

.info-value {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
}



.error-section {
  margin-top: 12px;
}

.toggle-error-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: transparent;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toggle-error-btn:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.error-message {
  margin-top: 8px;
  padding: 10px 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  font-size: 13px;
  color: #991b1b;
  line-height: 1.5;
}

.toggle-chunks-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 18px;
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast),
    background-color var(--transition-fast);
}

.toggle-chunks-btn:hover {
  background: var(--btn-primary-bg-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--btn-primary-bg) 30%, transparent);
}

.chunks-list {
  margin-top: 12px;
}

.chunks-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chunk-item {
  background: var(--glass-bg-light);
  border: 1px solid var(--glass-border-subtle);
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chunk-item:hover {
  background: rgba(255, 255, 255, 0.7);
  border-color: var(--glass-border);
  transform: translateX(4px);
}

.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.chunk-index {
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
}

.chunk-size {
  font-size: 12px;
  color: #9ca3af;
}

.chunk-preview {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-item {
  height: 20px;
  background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
