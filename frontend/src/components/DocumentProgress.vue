<template>
  <div class="doc-progress">
    <div class="doc-header">
      <Icon name="document" :size="14" />
      <span class="doc-filename">{{ document.filename }}</span>
      <button v-if="isTerminal" class="remove-btn" @click="emit('remove', document.id)">
        <Icon name="close" :size="12" />
      </button>
    </div>

    <div class="stepper">
      <div v-for="(step, index) in steps" :key="step.key" class="step-wrapper">
        <div :class="['step-dot', getStepClass(index)]">
          <Icon v-if="isCompleted(index)" name="check" :size="10" />
          <span v-else-if="isActive(index)" class="pulse-ring"></span>
        </div>
        <div v-if="index < steps.length - 1" :class="['step-line', getLineClass(index)]"></div>
      </div>
    </div>

    <div class="step-labels">
      <span
        v-for="(step, index) in steps"
        :key="'label-' + step.key"
        :class="['step-label', getStepClass(index)]"
      >
        {{ step.label }}
      </span>
    </div>

    <div class="status-desc" :class="statusClass">
      {{ statusDescription }}
    </div>

    <div v-if="document.status === 'ERROR'" class="error-area">
      <p class="error-text">{{ document.errorMessage || '处理失败' }}</p>
      <button v-if="refreshFn" class="retry-btn" @click="handleRetry">重试</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import Icon from './icons/Icon.vue'

const props = withDefaults(defineProps<{
  document: {
    id: number
    filename: string
    status: 'UPLOADING' | 'PARSING' | 'INDEXING' | 'READY' | 'ERROR'
    errorMessage?: string
  }
  autoPoll?: boolean
  refreshFn?: () => Promise<unknown>
}>(), {
  autoPoll: true
})

const emit = defineEmits<{
  'status-change': [status: string]
  'remove': [id: number]
}>()

const steps = [
  { key: 'UPLOADING', label: '上传', icon: 'upload', desc: '正在上传文件到服务器...' },
  { key: 'PARSING', label: '解析', icon: 'edit', desc: '正在解析文档内容...' },
  { key: 'INDEXING', label: '索引化', icon: 'database', desc: '正在生成向量索引...' },
  { key: 'READY', label: '就绪', icon: 'success', desc: '文档已就绪，可用于检索' }
]

const statusOrder: string[] = ['UPLOADING', 'PARSING', 'INDEXING', 'READY']

const currentErrorStep = ref<string>('UPLOADING')

function currentStepIndex(): number {
  if (props.document.status === 'ERROR') return statusOrder.indexOf(currentErrorStep.value)
  const idx = statusOrder.indexOf(props.document.status)
  return idx >= 0 ? idx : 0
}

function isCompleted(index: number): boolean {
  if (props.document.status === 'ERROR') return index < currentStepIndex()
  return index < statusOrder.indexOf(props.document.status)
}

function isActive(index: number): boolean {
  if (props.document.status === 'ERROR') return index === currentStepIndex()
  return index === statusOrder.indexOf(props.document.status)
}

function isPending(index: number): boolean {
  return !isCompleted(index) && !isActive(index)
}

function getStepClass(index: number): string {
  if (isCompleted(index)) return 'completed'
  if (isActive(index)) return props.document.status === 'ERROR' ? 'error-active' : 'active'
  return 'pending'
}

function getLineClass(index: number): string {
  if (isCompleted(index + 1)) return 'completed'
  if (isActive(index + 1)) return 'active'
  return 'pending'
}

const isTerminal = computed(() =>
  props.document.status === 'READY' || props.document.status === 'ERROR'
)

const statusDescriptions: Record<string, string> = {
  UPLOADING: '正在上传文件到服务器...',
  PARSING: '正在解析文档内容...',
  INDEXING: '正在生成向量索引...',
  READY: '✓ 文档已就绪',
  ERROR: props.document.errorMessage || '处理失败'
}

const statusDescription = computed(() => statusDescriptions[props.document.status] || '')

const statusClass = computed(() => {
  if (props.document.status === 'READY') return 'completed'
  if (props.document.status === 'ERROR') return 'error-active'
  return ''
})

let pollTimer: ReturnType<typeof setInterval> | null = null

watch(() => props.document.status, (newVal) => {
  emit('status-change', newVal)
}, { immediate: false })

onMounted(() => {
  if (props.autoPoll && !isTerminal.value && props.refreshFn) {
    startPolling()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      await props.refreshFn!()
    } catch {
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(isTerminal, (terminal) => {
  if (terminal) stopPolling()
  else if (props.autoPoll && props.refreshFn) startPolling()
})

async function handleRetry() {
  if (props.refreshFn) await props.refreshFn()
}
</script>

<style scoped>
.doc-progress {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  -webkit-backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-glass-sm);
  border-radius: var(--radius-md);
  padding: 12px;
  margin-bottom: 10px;
}

.doc-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.doc-filename {
  font-size: 12px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.remove-btn {
  padding: 2px;
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.remove-btn:hover {
  color: #ef4444;
}

.stepper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  padding: 0 2px;
}

.step-wrapper {
  display: flex;
  align-items: center;
  flex: 1;
}

.step-wrapper:first-child {
  margin-left: 0;
}

.step-wrapper:last-child {
  margin-right: 0;
}

.step-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 8px;
  transition: all 0.3s ease;
  position: relative;
}

.step-dot.completed {
  background: #16a34a;
  color: white;
}

.step-dot.active {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
}

.step-dot.pending {
  background: white;
  border: 2px solid #e5e5e5;
  color: transparent;
}

.step-dot.error-active {
  background: #ef4444;
  color: white;
}

.step-line {
  height: 2px;
  flex: 1;
  margin: 0 3px;
  transition: all 0.3s ease;
}

.step-line.completed {
  background: #16a34a;
}

.step-line.active {
  background: linear-gradient(90deg, #16a34a 0%, var(--btn-primary-bg) 100%);
}

.step-line.pending {
  background: #e5e5e5;
}

.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: color-mix(in srgb, var(--btn-primary-bg) 35%, transparent);
  animation: pulse 1.5s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

.step-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 2px;
  margin-bottom: 8px;
}

.step-label {
  font-size: 10px;
  text-align: center;
  flex: 1;
  transition: color 0.3s;
}

.step-label.completed,
.step-label.active {
  color: var(--text-primary);
  font-weight: 500;
}

.step-label.pending {
  color: #999;
}

.status-desc {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 4px;
  line-height: 1.4;
  color: #666;
}

.status-desc.completed {
  color: #16a34a;
  background: #f0fdf4;
}

.status-desc.error-active {
  color: #dc2626;
  background: #fef2f2;
}

.error-area {
  margin-top: 8px;
  padding: 8px;
  background: rgba(254, 242, 242, 0.7);
  backdrop-filter: blur(var(--blur-sm));
  -webkit-backdrop-filter: blur(var(--blur-sm));
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: 6px;
}

.error-text {
  font-size: 11px;
  color: #dc2626;
  line-height: 1.4;
  margin: 0 0 6px 0;
}

.retry-btn {
  font-size: 11px;
  padding: 4px 12px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.retry-btn:hover {
  background: #dc2626;
}
</style>
