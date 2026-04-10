<template>
  <div class="status-timeline">
    <div class="timeline-container">
      <div
        v-for="(stage, index) in stages"
        :key="stage.key"
        class="timeline-item"
        :class="{ 
          active: isStageActive(stage.key), 
          completed: isStageCompleted(stage.key),
          error: isStageError(stage.key)
        }"
      >
        <div class="stage-icon">
          <Icon v-if="isStageCompleted(stage.key)" name="check" :size="14" />
          <div v-else-if="isStageError(stage.key)" class="error-icon">✕</div>
          <div v-else-if="isStageActive(stage.key)" class="spinner"></div>
          <div v-else class="stage-number">{{ index + 1 }}</div>
        </div>
        <span class="stage-label">{{ stage.label }}</span>
      </div>
    </div>
    <div class="current-status">
      <span class="status-tag" :class="'status-' + status.toLowerCase()">
        {{ statusLabel }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@/components/icons'

const props = defineProps<{
  status: string
  errorMessage?: string | null
}>()

const stages = [
  { key: 'uploading', label: '上传' },
  { key: 'parsing', label: '解析' },
  { key: 'indexing', label: '索引' }
]

const statusLabel = computed(() => {
  const map: Record<string, string> = {
    UPLOADING: '上传中',
    PARSING: '解析中',
    INDEXING: '索引中',
    READY: '已完成',
    ERROR: '处理失败'
  }
  return map[props.status] || props.status
})

function getErrorStage(): string | null {
  if (props.status !== 'ERROR' || !props.errorMessage) {
    return null
  }
  const match = props.errorMessage.match(/^\[(\S+)阶段\]/)
  if (match) {
    const stageMap: Record<string, string> = {
      '上传': 'uploading',
      '解析': 'parsing',
      '索引': 'indexing'
    }
    return stageMap[match[1]] || null
  }
  return null
}

function isStageActive(stageKey: string): boolean {
  return props.status.toLowerCase() === stageKey
}

function isStageError(stageKey: string): boolean {
  const errorStage = getErrorStage()
  return errorStage === stageKey
}

function isStageCompleted(stageKey: string): boolean {
  const order = ['uploading', 'parsing', 'indexing']
  const currentIndex = order.indexOf(props.status.toLowerCase())
  const stageIndex = order.indexOf(stageKey)

  if (props.status === 'READY') {
    return true
  }

  if (props.status === 'ERROR') {
    const errorStage = getErrorStage()
    if (!errorStage) {
      return false
    }
    const errorStageIndex = order.indexOf(errorStage)
    return stageIndex < errorStageIndex
  }

  return stageIndex < currentIndex
}
</script>

<style scoped>
.status-timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-container {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.timeline-container::before {
  content: '';
  position: absolute;
  top: 12px;
  left: 40px;
  right: 40px;
  height: 2px;
  background: #e5e7eb;
  z-index: 0;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 1;
  flex: 1;
}

.stage-icon {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #f3f4f6;
  border: 2px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  transition: all 0.3s ease;
}

.timeline-item.completed .stage-icon {
  background: #dcfce7;
  border-color: #16a34a;
  color: #16a34a;
}

.timeline-item.active .stage-icon {
  background: #dbeafe;
  border-color: #1d4ed8;
  color: #1d4ed8;
}

.stage-number {
  font-size: 12px;
  font-weight: 600;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top-color: #1d4ed8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.stage-label {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
  transition: color 0.3s ease;
}

.timeline-item.completed .stage-label {
  color: #16a34a;
}

.timeline-item.active .stage-label {
  color: #1d4ed8;
}

.timeline-item.error .stage-icon {
  background: #fee2e2;
  border-color: #dc2626;
  color: #dc2626;
}

.timeline-item.error .stage-label {
  color: #dc2626;
}

.error-icon {
  font-size: 14px;
  font-weight: bold;
  line-height: 1;
}

.current-status {
  display: flex;
  justify-content: center;
}

.status-tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 6px;
  font-weight: 500;
  display: inline-block;
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
</style>
