<template>
  <div class="extra-config-section">
    <div class="section-header">
      <label class="section-title">自定义配置</label>
      <span class="section-hint">（选填，可多选）</span>
    </div>

    <div class="template-selector">
      <div class="template-chips">
        <button
          v-for="template in templates"
          :key="template.id"
          type="button"
          class="template-chip"
          :class="{ active: selectedTemplates.has(template.id) }"
          @click="toggleTemplate(template)"
        >
          {{ template.name }}
        </button>
        <button
          type="button"
          class="template-chip template-chip-custom"
          :class="{ active: isCustomMode }"
          @click="enterCustomMode"
        >
          自定义
        </button>
      </div>
      <button
        v-if="selectedTemplates.size > 0"
        type="button"
        class="clear-all-btn"
        @click="clearAll"
      >
        清除全部
      </button>
    </div>

    <div class="editor-area" v-if="selectedTemplates.size > 0 || isCustomMode">
      <AutoResizeTextarea
        ref="textareaRef"
        v-model="jsonText"
        :min-rows="3"
        :max-rows="12"
        :max-height="320"
        enter-behavior="newline"
        :auto-resize="true"
        :placeholder="templatePlaceholder"
        class="json-textarea"
        :class="{ error: !!error }"
        @input="onInput"
        @focus="enterCustomMode"
      />

      <div class="editor-toolbar">
        <button type="button" class="toolbar-btn" @click="formatJson" title="格式化">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 7h16M4 12h10M4 17h6"/>
          </svg>
          格式化
        </button>
        <button type="button" class="toolbar-btn" @click="clearJson" title="清空">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12"/>
          </svg>
          清空
        </button>
      </div>

      <div v-if="error" class="error-tip">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        {{ error }}
      </div>
    </div>

    <div v-if="isValid && mergedPreview" class="preview-section">
      <div class="preview-header">
        <span class="preview-title">请求预览</span>
        <span class="preview-hint">绿色高亮为自定义追加的参数</span>
      </div>
      <pre class="preview-code"><code v-html="highlightedPreview"></code></pre>
    </div>

    <div class="config-hint">
      <span>注意：自定义配置会合并到 LLM 请求的根结构下，可覆盖默认参数</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import AutoResizeTextarea from '@/components/AutoResizeTextarea.vue'

interface Props {
  modelValue?: string
  modelName?: string
  temperature?: number
  maxTokens?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  modelName: 'qwen-max',
  temperature: 0.7,
  maxTokens: 4096
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

interface Template {
  id: string
  name: string
  config: string
}

const templates: Template[] = [
  {
    id: 'thinking',
    name: '深度思考',
    config: `{
  "thinking": {
    "type": "enabled"
  }
}`
  },
  {
    id: 'thinking_budget',
    name: '思考预算',
    config: `{
  "thinking_budget": 2048
}`
  },
  {
    id: 'json_mode',
    name: 'JSON Mode',
    config: `{
  "response_format": {
    "type": "json_object"
  }
}`
  },
  {
    id: 'reasoning',
    name: '推理参数',
    config: `{
  "reasoning": {
    "effort": "medium",
    "budget": 2048
  }
}`
  }
]

const selectedTemplates = ref<Set<string>>(new Set())
const isCustomMode = ref(false)
const jsonText = ref(props.modelValue)
const error = ref('')
const textareaRef = ref<InstanceType<typeof AutoResizeTextarea>>()

const templatePlaceholder = computed(() => {
  if (isCustomMode.value) {
    return '{\n  "key": "value"\n}'
  }
  if (selectedTemplates.value.size === 0) {
    return '{\n  "key": "value"\n}'
  }
  const template = templates.find(t => t.id === [...selectedTemplates.value][0])
  return template ? template.config : '{\n  "key": "value"\n}'
})

const isValid = computed(() => {
  if (!jsonText.value.trim()) return false
  try {
    JSON.parse(jsonText.value)
    return true
  } catch {
    return false
  }
})

const previewObject = computed(() => {
  if (!isValid.value) return null
  try {
    return JSON.parse(jsonText.value)
  } catch {
    return null
  }
})

const mergedPreview = computed(() => {
  if (!previewObject.value) return null
  return previewObject.value
})

function buildHighlightedJson(obj: Record<string, unknown>, isExtra: boolean, depth: number = 1): string {
  const indent = '  '.repeat(depth)
  const lines: string[] = []

  Object.entries(obj).forEach(([key, value], idx) => {
    const isLast = idx === Object.entries(obj).length - 1
    const comma = isLast ? '' : ','

    if (value && typeof value === 'object' && !Array.isArray(value)) {
      const nestedObj = value as Record<string, unknown>
      const keyClass = isExtra ? 'json-extra' : 'json-key'
      const nested = buildHighlightedJson(nestedObj, true, depth + 1)
      lines.push(`${indent}<span class="${keyClass}">"${key}"</span>: {\n${nested}\n${indent}}${comma}`)
    } else if (Array.isArray(value)) {
      const keyClass = isExtra ? 'json-extra' : 'json-key'
      const items = value.map(item => JSON.stringify(item)).join(', ')
      lines.push(`${indent}<span class="${keyClass}">"${key}"</span>: [${items}]${comma}`)
    } else {
      const keyClass = isExtra ? 'json-extra' : 'json-key'
      let valueHtml = ''
      if (typeof value === 'string') {
        valueHtml = `<span class="json-string">"${value}"</span>`
      } else if (typeof value === 'number') {
        valueHtml = `<span class="json-number">${value}</span>`
      } else if (typeof value === 'boolean') {
        valueHtml = `<span class="json-boolean">${value}</span>`
      } else if (value === null) {
        valueHtml = `<span class="json-null">null</span>`
      }
      lines.push(`${indent}<span class="${keyClass}">"${key}"</span>: ${valueHtml}${comma}`)
    }
  })

  return lines.join('\n')
}

const highlightedPreview = computed(() => {
  if (!previewObject.value) return ''

  const preview: Record<string, unknown> = {
    model: props.modelName,
    messages: [{ role: "user", content: "" }],
    temperature: props.temperature,
    max_tokens: props.maxTokens,
    stream: true,
    ...previewObject.value
  }

  const extraKeys = new Set(Object.keys(previewObject.value))

  const standardObj: Record<string, unknown> = {}
  const extraObj: Record<string, unknown> = {}

  Object.entries(preview).forEach(([key, value]) => {
    if (extraKeys.has(key)) {
      extraObj[key] = value
    } else {
      standardObj[key] = value
    }
  })

  const lines: string[] = ['{']

  Object.entries(standardObj).forEach(([key, value], idx) => {
    const isLast = idx === Object.entries(standardObj).length - 1 && Object.keys(extraObj).length === 0
    const comma = isLast ? '' : ','

    if (value && typeof value === 'object' && !Array.isArray(value)) {
      const nestedObj = value as Record<string, unknown>
      const nested = buildHighlightedJson(nestedObj, false, 2)
      lines.push(`  <span class="json-key">"${key}"</span>: {\n${nested}\n  }${comma}`)
    } else if (Array.isArray(value)) {
      const items = (value as unknown[]).map(item => JSON.stringify(item)).join(', ')
      lines.push(`  <span class="json-key">"${key}"</span>: [${items}]${comma}`)
    } else {
      let valueHtml = ''
      if (typeof value === 'string') {
        valueHtml = `<span class="json-string">"${value}"</span>`
      } else if (typeof value === 'number') {
        valueHtml = `<span class="json-number">${value}</span>`
      } else if (typeof value === 'boolean') {
        valueHtml = `<span class="json-boolean">${value}</span>`
      } else if (value === null) {
        valueHtml = `<span class="json-null">null</span>`
      }
      lines.push(`  <span class="json-key">"${key}"</span>: ${valueHtml}${comma}`)
    }
  })

  Object.entries(extraObj).forEach(([key, value], idx) => {
    const isLast = idx === Object.entries(extraObj).length - 1
    const comma = isLast ? '' : ','

    if (value && typeof value === 'object' && !Array.isArray(value)) {
      const nestedObj = value as Record<string, unknown>
      const nested = buildHighlightedJson(nestedObj, true, 2)
      lines.push(`  <span class="json-extra">"${key}"</span>: {\n${nested}\n  }${comma}`)
    } else if (Array.isArray(value)) {
      const items = (value as unknown[]).map(item => JSON.stringify(item)).join(', ')
      lines.push(`  <span class="json-extra">"${key}"</span>: [${items}]${comma}`)
    } else {
      let valueHtml = ''
      if (typeof value === 'string') {
        valueHtml = `<span class="json-string">"${value}"</span>`
      } else if (typeof value === 'number') {
        valueHtml = `<span class="json-number">${value}</span>`
      } else if (typeof value === 'boolean') {
        valueHtml = `<span class="json-boolean">${value}</span>`
      } else if (value === null) {
        valueHtml = `<span class="json-null">null</span>`
      }
      lines.push(`  <span class="json-extra">"${key}"</span>: ${valueHtml}${comma}`)
    }
  })

  lines.push('}')
  return lines.join('\n')
})

watch(
  () => props.modelValue,
  (val) => {
    if (val !== jsonText.value) {
      jsonText.value = val
      syncTemplatesFromValue()
    }
  }
)

watch(jsonText, (val) => {
  if (!isCustomMode.value) {
    emit('update:modelValue', val)
  }
})

watch(selectedTemplates, () => {
  updateJsonFromTemplates()
}, { deep: true })

function syncTemplatesFromValue() {
  if (!props.modelValue.trim()) {
    selectedTemplates.value.clear()
    isCustomMode.value = false
    return
  }

  try {
    const parsed = JSON.parse(props.modelValue)
    const matchedIds = templates
      .filter(t => {
        try {
          const templateObj = JSON.parse(t.config)
          return JSON.stringify(templateObj) === JSON.stringify(parsed)
        } catch {
          return false
        }
      })
      .map(t => t.id)

    if (matchedIds.length > 0 && matchedIds.length < templates.length) {
      selectedTemplates.value = new Set(matchedIds)
      isCustomMode.value = false
    } else {
      selectedTemplates.value.clear()
      isCustomMode.value = true
    }
  } catch {
    selectedTemplates.value.clear()
    isCustomMode.value = true
  }
}

function toggleTemplate(template: Template) {
  isCustomMode.value = false
  if (selectedTemplates.value.has(template.id)) {
    selectedTemplates.value.delete(template.id)
  } else {
    selectedTemplates.value.add(template.id)
  }
  selectedTemplates.value = new Set(selectedTemplates.value)
}

function enterCustomMode() {
  isCustomMode.value = true
}

function updateJsonFromTemplates() {
  if (isCustomMode.value) return

  if (selectedTemplates.value.size === 0) {
    jsonText.value = ''
    error.value = ''
    return
  }

  try {
    const merged: Record<string, unknown> = {}
    for (const id of selectedTemplates.value) {
      const template = templates.find(t => t.id === id)
      if (template) {
        const templateObj = JSON.parse(template.config)
        Object.assign(merged, templateObj)
      }
    }
    jsonText.value = JSON.stringify(merged, null, 2)
    error.value = ''
  } catch (e) {
    error.value = '模板合并失败'
  }
}

function onInput() {
  isCustomMode.value = true
  emit('update:modelValue', jsonText.value)
  validateJson()
}

function validateJson(): boolean {
  if (!jsonText.value.trim()) {
    error.value = ''
    return true
  }
  try {
    JSON.parse(jsonText.value)
    error.value = ''
    return true
  } catch {
    error.value = 'JSON 格式错误'
    return false
  }
}

function formatJson() {
  if (!jsonText.value.trim()) return
  try {
    const parsed = JSON.parse(jsonText.value)
    jsonText.value = JSON.stringify(parsed, null, 2)
    error.value = ''
  } catch {
    error.value = '格式化失败'
  }
}

function clearJson() {
  jsonText.value = ''
  error.value = ''
  selectedTemplates.value.clear()
  isCustomMode.value = false
}

function clearAll() {
  jsonText.value = ''
  error.value = ''
  selectedTemplates.value.clear()
  isCustomMode.value = false
}

defineExpose({
  validateJson
})
</script>

<style scoped>
.extra-config-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: #444;
}

.section-hint {
  font-size: 12px;
  color: #999;
}

.template-selector {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.template-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.template-chip {
  padding: 6px 14px;
  border: 1px solid #e5e5e5;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  color: #666;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.template-chip:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.template-chip.active {
  border-color: var(--color-accent);
  background: color-mix(in srgb, var(--color-accent) 8%, transparent);
  color: var(--color-accent);
}

.template-chip-custom {
  border-style: dashed;
}

.template-chip-custom.active {
  border-style: solid;
}

.clear-all-btn {
  padding: 4px 10px;
  border: none;
  border-radius: 12px;
  font-size: 11px;
  color: #999;
  background: transparent;
  cursor: pointer;
  transition: color 0.2s ease;
}

.clear-all-btn:hover {
  color: #ef4444;
  text-decoration: underline;
}

.editor-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.json-textarea :deep(.auto-resize-textarea) {
  background: #faf9f7;
}

.json-textarea :deep(.textarea-inner) {
  font-family: 'SF Mono', 'Fira Code', Monaco, 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.json-textarea.error :deep(.auto-resize-textarea) {
  border-color: #ef4444;
}

.editor-toolbar {
  display: flex;
  gap: 8px;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #666;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toolbar-btn:hover {
  border-color: #ccc;
  color: #333;
  background: #f8f8f8;
}

.error-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
  color: #ef4444;
  background: #fef2f2;
}

.preview-section {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8f9fa;
  border: 1px solid #e8e8e8;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.preview-title {
  font-size: 12px;
  font-weight: 500;
  color: #666;
}

.preview-hint {
  font-size: 11px;
  color: #999;
}

.preview-code {
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: white;
  border: 1px solid #e5e5e5;
  font-family: 'SF Mono', 'Fira Code', Monaco, 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.preview-code :deep(.json-key) {
  color: #0550ae;
}

.preview-code :deep(.json-string) {
  color: #0a3069;
}

.preview-code :deep(.json-number) {
  color: #0550ae;
}

.preview-code :deep(.json-boolean) {
  color: #0550ae;
}

.preview-code :deep(.json-null) {
  color: #0550ae;
}

.preview-code :deep(.json-extra) {
  color: #16a34a;
}

.config-hint {
  font-size: 11px;
  color: #999;
}
</style>
