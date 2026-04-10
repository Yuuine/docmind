<template>
  <div
    class="documents-page"
    @dragover.prevent="onGlobalDragOver"
    @dragleave.prevent="onGlobalDragLeave"
    @drop.prevent="onGlobalDrop"
  >
    <div class="page-header">
      <div class="search-bar">
        <input
          v-model="localSearchQuery"
          type="text"
          placeholder="搜索文件名..."
          class="search-input"
        />
      </div>
      <button v-if="documents.length > 0 || searchQuery" class="upload-btn" @click="triggerUpload">
        <Icon name="upload" :size="16" />
        <span class="upload-btn-text">上传</span>
      </button>
      <input ref="fileInputRef" type="file" accept=".pdf,.doc,.docx,.txt,.md" hidden @change="onFileSelected" />
    </div>

    <div v-if="documents.length === 0 && !searchQuery" class="empty-wrapper">
      <div
        class="upload-zone"
        :class="{ 'is-uploading': isUploading }"
        @click="triggerUpload"
        @dragover.prevent="onDragOver"
        @dragleave.prevent="onDragLeave"
        @drop.prevent="onDrop"
      >
        <Icon name="upload" :size="32" />
        <p class="upload-hint">点击或拖拽文件至此上传</p>
        <p class="upload-formats">支持 PDF、DOC、DOCX、TXT、MD 格式</p>
        <div v-if="isUploading && uploadProgress > 0" class="progress-bar">
          <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
        </div>
      </div>
    </div>

    <div v-else class="content-wrapper">
      <div class="doc-list">
        <div
          v-for="(doc, index) in documents"
          :key="doc.id"
          class="doc-row"
        >
          <div class="doc-card" :class="{ 'is-expanded': isExpanded(doc.id) }" @click="toggleExpand(doc.id)">
            <div class="doc-info">
              <Icon name="document" :size="20" />
              <span class="doc-filename">{{ doc.filename }}</span>
              <Icon :name="isExpanded(doc.id) ? 'chevron-up' : 'chevron-down'" :size="16" class="expand-icon" />
            </div>
            <div class="doc-meta">
              <span>{{ formatFileSize(doc.fileSize ?? 0) }}</span>
              <span>{{ formatDate(doc.createdAt ?? '') }}</span>
            </div>
            <div class="doc-actions">
              <span
                class="status-tag"
                :class="'status-' + (doc.status ?? '').toLowerCase()"
                >{{ statusLabel(doc.status ?? '') }}</span
              >
              <button
                class="download-btn"
                @click.stop="handleDownload(doc)"
                title="下载"
              >
                <Icon name="download" :size="16" />
              </button>
              <button
                class="delete-btn"
                @click.stop="confirmDelete(doc)"
                title="删除"
              >
                <Icon name="trash" :size="16" />
              </button>
            </div>
          </div>
          <transition name="fade-slide">
            <DocumentDetailPanel v-if="isExpanded(doc.id)" :document="doc" />
          </transition>
        </div>
      </div>

      <div v-if="total > 0" class="pagination">
        <div class="pagination-left">
          <span class="pagination-info">共 {{ total }} 条记录</span>
        </div>
        <div class="pagination-right">
          <button 
            class="pagination-btn" 
            :disabled="currentPage <= 1"
            @click="setPage(currentPage - 1)"
          >
            &lt;
          </button>
          <div class="pagination-pages">
            <button
              v-for="page in getDisplayPages()"
              :key="page"
              class="page-btn"
              :class="{ active: page === currentPage }"
              @click="setPage(page)"
            >
              {{ page }}
            </button>
          </div>
          <button 
            class="pagination-btn" 
            :disabled="currentPage >= totalPages"
            @click="setPage(currentPage + 1)"
          >
            &gt;
          </button>
        </div>
      </div>
    </div>

    <div v-if="isDragging && (documents.length > 0 || searchQuery)" class="drop-overlay">
      <div class="drop-overlay-content">
        <Icon name="upload" :size="48" />
        <p class="drop-overlay-text">释放文件以上传</p>
      </div>
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
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { documentApi } from '@/api'
import { useToastStore } from '@/stores/toast'
import { useUserStore } from '@/stores/user'
import { Icon } from '@/components/icons'
import ConfirmModal from '@/components/ConfirmModal.vue'
import DocumentDetailPanel from '@/components/DocumentDetailPanel.vue'
import { useDocumentsList } from '@/composables/useDocumentsList'
import { useSmartPolling } from '@/composables/useSmartPolling'
import { useDebouncedSearch } from '@/composables/useDebouncedSearch'
import { formatFileSize, formatDate, statusLabel } from '@/utils/format'
import type { Document } from '@/types'

const toastStore = useToastStore()
const userStore = useUserStore()
const { documents, loadDocuments, currentPage, pageSize, total, totalPages, setPage, searchQuery, setSearchQuery } = useDocumentsList()

const uploadProgress = ref(0)
const isUploading = ref(false)
const isDragging = ref(false)
const showDeleteDocModal = ref(false)
const targetDeleteDoc = ref<Document | null>(null)
const fileInputRef = ref<HTMLInputElement>()
const expandedDocIds = ref<Record<number, boolean>>({})
let globalDragCounter = 0
let localDragCounter = 0

const { localSearchQuery, cleanup: cleanupSearch } = useDebouncedSearch({
  delay: 300,
  onSearch: setSearchQuery
})

function hasProcessingDocuments(): boolean {
  return documents.value.some(doc => 
    doc.status === 'UPLOADING' || 
    doc.status === 'PARSING' || 
    doc.status === 'INDEXING'
  )
}

const {
  startPolling,
  stopPolling,
  restartPollingIfNeeded,
  setupVisibilityListener,
  cleanup: cleanupPolling
} = useSmartPolling({
  fastInterval: 2000,
  slowInterval: 60000,
  hasProcessing: hasProcessingDocuments,
  onPoll: loadDocuments
})

function toggleExpand(docId: number) {
  expandedDocIds.value[docId] = !expandedDocIds.value[docId]
}

function isExpanded(docId: number): boolean {
  return !!expandedDocIds.value[docId]
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileUpload(file: File) {
  isUploading.value = true
  uploadProgress.value = 10
  try {
    uploadProgress.value = 50
    const userId = userStore.user?.id
    await documentApi.upload(file, userId)
    uploadProgress.value = 100
    toastStore.success('文件上传成功')
    setTimeout(() => {
      uploadProgress.value = 0
      isUploading.value = false
    }, 1000)
    await loadDocuments()
  } catch (error) {
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

function onDragOver() {
  localDragCounter++
}

function onDragLeave() {
  localDragCounter--
}

function onDrop(e: DragEvent) {
  localDragCounter = 0
  const file = e.dataTransfer?.files[0]
  if (file) handleFileUpload(file)
}

function onGlobalDragOver() {
  globalDragCounter++
  if (globalDragCounter === 1) {
    isDragging.value = true
  }
}

function onGlobalDragLeave() {
  globalDragCounter--
  if (globalDragCounter === 0) {
    isDragging.value = false
  }
}

function onGlobalDrop(e: DragEvent) {
  globalDragCounter = 0
  isDragging.value = false
  const file = e.dataTransfer?.files[0]
  if (file) handleFileUpload(file)
}

function getDisplayPages(): number[] {
  const pages: number[] = []
  const maxVisible = 7
  let startPage = Math.max(1, currentPage.value - Math.floor(maxVisible / 2))
  let endPage = startPage + maxVisible - 1
  
  if (endPage > totalPages.value) {
    endPage = totalPages.value
    startPage = Math.max(1, endPage - maxVisible + 1)
  }
  
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i)
  }
  
  return pages
}

function confirmDelete(doc: Document) {
  targetDeleteDoc.value = doc
  showDeleteDocModal.value = true
}

async function executeDelete() {
  if (!targetDeleteDoc.value) return
  try {
    const userId = userStore.user?.id
    await documentApi.delete(targetDeleteDoc.value.id, userId)
    toastStore.success('文件已删除')
    await loadDocuments()
  } catch {
    toastStore.error('删除失败')
  } finally {
    targetDeleteDoc.value = null
  }
}

async function handleDownload(doc: Document) {
  try {
    const blob = await documentApi.download(doc.id) as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = doc.filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    toastStore.success('文件下载成功')
  } catch {
    toastStore.error('文件下载失败')
  }
}

watch(documents, () => {
  restartPollingIfNeeded()
})

onMounted(() => {
  loadDocuments()
  startPolling()
  setupVisibilityListener()
})

onBeforeUnmount(() => {
  cleanupPolling()
  cleanupSearch()
})
</script>

<style scoped>
.documents-page {
  height: calc(100vh - 56px);
  background: transparent;
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 32px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 16px;
  flex-shrink: 0;
}

.search-bar {
  flex: 1;
  max-width: 600px;
}

.search-input {
  width: 100%;
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
  transition:
    border-color 0.2s ease,
    background 0.2s ease,
    box-shadow 0.2s ease;
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.7);
  border-color: var(--glass-border);
  box-shadow: 0 0 0 3px rgba(26, 26, 26, 0.04);
}

.search-input::placeholder {
  color: #aaa;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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

.upload-btn:hover {
  background: var(--btn-primary-bg-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--btn-primary-bg) 30%, transparent);
}

.upload-btn:active {
  transform: scale(0.98);
}

.upload-btn-text {
  display: inline-block;
}

.empty-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-zone {
  background: var(--glass-bg-light);
  backdrop-filter: blur(var(--blur-sm));
  -webkit-backdrop-filter: blur(var(--blur-sm));
  border: 2px dashed var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 48px 24px;
  text-align: center;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
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

.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.doc-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 8px;
  margin-right: -8px;
}

.doc-list::-webkit-scrollbar {
  width: 6px;
}

.doc-list::-webkit-scrollbar-track {
  background: transparent;
}

.doc-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

.doc-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

.doc-row {
  box-sizing: border-box;
  flex-shrink: 0;
}

.doc-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  -webkit-backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-glass-sm);
  border-radius: var(--radius-md);
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast),
    background var(--transition-fast);
}

.doc-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glass-md);
  background: rgba(255, 255, 255, 0.72);
}

.doc-card.is-expanded {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.doc-card.is-expanded:hover {
  transform: none;
}

.doc-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  position: relative;
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

.download-btn {
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

.download-btn:hover {
  color: #2563eb;
  background: #dbeafe;
}

.delete-btn:hover {
  color: #dc2626;
  background: #fee2e2;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding: 14px 20px;
  background: var(--glass-bg);
  backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  -webkit-backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-glass-sm);
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.pagination-info {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.pagination-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border: 1.5px solid #d1d5db;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 40px;
}

.pagination-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
  color: #1f2937;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.pagination-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: none;
}

.pagination-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination-pages {
  display: flex;
  gap: 6px;
}

.page-btn {
  min-width: 38px;
  height: 38px;
  padding: 0 12px;
  border: 1.5px solid transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover {
  background: #f3f4f6;
  color: #1f2937;
  border-color: #e5e7eb;
}

.page-btn.active {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border-color: var(--btn-primary-bg);
  box-shadow: 0 2px 8px color-mix(in srgb, var(--btn-primary-bg) 25%, transparent);
}

.page-btn:active:not(.active) {
  transform: scale(0.97);
}

.drop-overlay {
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

.drop-overlay-content {
  background: white;
  border-radius: 16px;
  padding: 48px 64px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: scaleIn 0.15s ease;
}

.drop-overlay-text {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 16px 0 0;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.expand-icon {
  margin-left: auto;
  color: #9ca3af;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.doc-card:hover .expand-icon {
  color: #6b7280;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-slide-enter-to,
.fade-slide-leave-from {
  opacity: 1;
  transform: translateY(0);
}
</style>
