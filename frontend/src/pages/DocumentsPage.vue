<template>
  <div class="documents-page">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <h1 class="page-title">文档管理</h1>
      <button class="upload-btn" @click="triggerUpload">
        <Icon name="upload" :size="16" />
        上传文档
      </button>
      <input ref="fileInputRef" type="file" accept=".pdf,.doc,.docx,.txt,.md" hidden @change="onFileSelected" />
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索文件名..."
        class="search-input"
      />
    </div>

    <!-- 上传区域 -->
    <div
      class="upload-zone"
      :class="{ 'is-uploading': isUploading }"
      @click="triggerUpload"
      @dragover.prevent="onDragOver"
      @dragleave.prevent="onDragLeave"
      @drop.prevent="onDrop"
    >
      <Icon name="upload" :size="32" />
      <p class="upload-hint">点击或拖拽文件上传</p>
      <p class="upload-formats">支持 PDF、DOC、DOCX、TXT、MD 格式</p>
      <div v-if="isUploading && uploadProgress > 0" class="progress-bar">
        <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
      </div>
    </div>

    <!-- 文档列表区域 -->
    <div v-if="filteredDocuments.length > 0" class="doc-list">
      <div v-for="doc in filteredDocuments" :key="doc.id" class="doc-card">
        <div class="doc-info">
          <Icon name="document" :size="20" />
          <span class="doc-filename">{{ doc.filename }}</span>
        </div>
        <div class="doc-meta">
          <span>{{ formatFileSize(doc.fileSize) }}</span>
          <span>{{ formatDate(doc.createdAt) }}</span>
        </div>
        <div class="doc-actions">
          <span class="status-tag" :class="'status-' + doc.status.toLowerCase()">{{ statusLabel(doc.status) }}</span>
          <button class="delete-btn" @click.stop="confirmDelete(doc)" title="删除">
            <Icon name="trash" :size="16" />
          </button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <Icon name="document" :size="48" />
      <p class="empty-title">暂无文档</p>
      <p class="empty-desc">上传您的第一个文档开始使用</p>
    </div>

    <ConfirmModal
      v-model="showDeleteDocModal"
      title="删除文件"
      :message="`确定要删除文件「${targetDeleteDoc?.filename || ''}」吗？此操作不可撤销。`"
      confirm-text="确定删除"
      cancel-text="取消"
      :is-destructive="true"
      @confirm="executeDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { documentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToastStore } from '@/stores/toast'
import { Icon } from '@/components/icons'
import ConfirmModal from '@/components/ConfirmModal.vue'
import type { Document } from '@/types'

const userStore = useUserStore()
const toastStore = useToastStore()

const documents = ref<Document[]>([])
const searchQuery = ref('')
const uploadProgress = ref(0)
const isUploading = ref(false)
const showDeleteDocModal = ref(false)
const targetDeleteDoc = ref<Document | null>(null)
const fileInputRef = ref<HTMLInputElement>()

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    UPLOADING: '上传中',
    PARSING: '解析中',
    INDEXING: '索引中',
    READY: '就绪',
    ERROR: '错误'
  }
  return map[status] || status
}

async function loadDocuments() {
  const userId = userStore.user?.id
  if (!userId) return
  const result = await documentApi.list({ page: 1, size: 100 }) as any
  documents.value = result.items || result || []
}

const filteredDocuments = computed(() => {
  if (!searchQuery.value.trim()) return documents.value
  const q = searchQuery.value.toLowerCase()
  return documents.value.filter(d => d.filename.toLowerCase().includes(q))
})

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileUpload(file: File) {
  isUploading.value = true
  uploadProgress.value = 10
  try {
    uploadProgress.value = 50
    await documentApi.upload(file)
    uploadProgress.value = 100
    toastStore.success('文件上传成功')
    setTimeout(() => { uploadProgress.value = 0; isUploading.value = false }, 1000)
    await loadDocuments()
  } catch {
    toastStore.error('文件上传失败')
    uploadProgress.value = 0
    isUploading.value = false
  }
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) handleFileUpload(file)
  input.value = ''
}

let dragCounter = 0

function onDragOver() {
  dragCounter++
}

function onDragLeave() {
  dragCounter--
}

function onDrop(e: DragEvent) {
  dragCounter = 0
  const file = e.dataTransfer?.files[0]
  if (file) handleFileUpload(file)
}

function confirmDelete(doc: Document) {
  targetDeleteDoc.value = doc
  showDeleteDocModal.value = true
}

async function executeDelete() {
  if (!targetDeleteDoc.value) return
  try {
    await documentApi.delete(targetDeleteDoc.value.id)
    toastStore.success('文件已删除')
    await loadDocuments()
  } catch {
    toastStore.error('删除失败')
  } finally {
    targetDeleteDoc.value = null
  }
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.documents-page {
  min-height: 100vh;
  background: transparent;
  max-width: 960px;
  margin: 0 auto;
  padding: 32px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* 页面标题栏 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: transform var(--transition-fast),
              box-shadow var(--transition-fast),
              background-color var(--transition-fast);
}

.upload-btn:hover {
  background: var(--btn-primary-bg-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--btn-primary-bg) 30%, transparent);
}

.upload-btn:active {
  transform: scale(0.98);
}

/* 搜索栏 */
.search-bar {
  margin-bottom: 20px;
}

.search-input {
  width: 100%;
  max-width: 600px;
  padding: 10px 16px;
  border: 1.5px solid var(--glass-border-subtle);
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  background: var(--glass-bg-light);
  backdrop-filter: blur(var(--blur-sm));
  -webkit-backdrop-filter: blur(var(--blur-sm));
  color: #1a1a1a;
  box-sizing: border-box;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.7);
  border-color: var(--glass-border);
  box-shadow: 0 0 0 3px rgba(26, 26, 26, 0.04);
}

.search-input::placeholder {
  color: #aaa;
}

/* 上传区域 */
.upload-zone {
  background: var(--glass-bg-light);
  backdrop-filter: blur(var(--blur-sm));
  -webkit-backdrop-filter: blur(var(--blur-sm));
  border: 2px dashed var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 40px 20px;
  text-align: center;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 24px;
  position: relative;
}

.upload-zone:hover,
.upload-zone.is-uploading {
  border-color: rgba(26, 26, 26, 0.2);
  background: rgba(255, 255, 255, 0.5);
}

.upload-zone .upload-hint {
  font-size: 15px;
  font-weight: 500;
  color: #444;
  margin: 12px 0 6px;
}

.upload-zone .upload-formats {
  font-size: 13px;
  color: #999;
  margin: 0;
}

/* 进度条 */
.progress-bar {
  height: 6px;
  background: #f5f5f5;
  border-radius: 3px;
  overflow: hidden;
  margin-top: 12px;
}

.progress-fill {
  height: 100%;
  background: var(--btn-primary-bg);
  border-radius: 3px;
  transition: width 0.3s ease;
}

/* 文档列表 */
.doc-list {
  display: flex;
  flex-direction: column;
}

.doc-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  -webkit-backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-glass-sm);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  transition: transform var(--transition-fast),
              box-shadow var(--transition-fast),
              background var(--transition-fast);
}

.doc-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glass-md);
  background: rgba(255, 255, 255, 0.72);
}

.doc-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.doc-info svg {
  flex-shrink: 0;
  color: #666;
}

.doc-filename {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #888;
  flex-shrink: 0;
}

.doc-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

/* 状态标签 */
.status-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 6px;
  font-weight: 500;
  white-space: nowrap;
}

.status-uploading {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-parsing {
  background: #fef3c7;
  color: #d97706;
}

.status-indexing {
  background: #e0e7ff;
  color: #4338ca;
}

.status-ready {
  background: #dcfce7;
  color: #16a34a;
}

.status-error {
  background: #fee2e2;
  color: #dc2626;
}

/* 删除按钮 */
.delete-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #999;
  cursor: pointer;
  transition: all 0.2s ease;
}

.delete-btn:hover {
  color: #dc2626;
  background: #fee2e2;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #bbb;
}

.empty-state svg {
  color: #ccc;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #888;
  margin: 16px 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: #aaa;
  margin: 0;
}
</style>
