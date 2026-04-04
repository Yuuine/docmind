<template>
  <form @submit.prevent="handleSubmit" class="model-form">
    <div class="form-group">
      <label>模型名称 <span class="required">*</span></label>
      <input
        v-model="formData.name"
        type="text"
        placeholder="如 Qwen3-Max"
        :class="{ error: errors.name }"
      />
    </div>

    <div class="form-group">
      <label>API 地址 <span class="required">*</span></label>
      <input
        v-model="formData.baseUrl"
        type="text"
        placeholder="https://api.example.com"
        :class="{ error: errors.baseUrl }"
      />
    </div>

    <div class="form-group">
      <label>API 密钥 <span class="required">*</span></label>
      <input
        v-model="formData.apiKey"
        type="password"
        placeholder="sk-..."
        :class="{ error: errors.apiKey }"
      />
    </div>

    <div class="form-group">
      <label>模型标识符 <span class="required">*</span></label>
      <input
        v-model="formData.modelName"
        type="text"
        placeholder="如 qwen-max"
        :class="{ error: errors.modelName }"
      />
    </div>

    <div class="form-row">
      <div class="form-group half">
        <label>最大 Token 数</label>
        <input
          v-model.number="formData.maxTokens"
          type="number"
          min="1"
          placeholder="4096"
        />
      </div>
      <div class="form-group half">
        <label>温度参数</label>
        <input
          v-model.number="formData.temperature"
          type="number"
          min="0"
          max="2"
          step="0.1"
          placeholder="0.7"
        />
      </div>
    </div>

    <div class="advanced-config">
      <div class="advanced-config-header" @click="showAdvancedConfig = !showAdvancedConfig">
        <span>高级配置</span>
        <span class="advanced-chevron" :class="{ expanded: showAdvancedConfig }">▶</span>
      </div>
      <transition name="accordion">
        <div v-if="showAdvancedConfig" class="advanced-config-body">
          <div class="form-group">
            <label>提供商类型</label>
            <select v-model="formData.providerType">
              <option value="">自定义</option>
              <option value="DEEPSEEK">DeepSeek</option>
              <option value="OPENAI">OpenAI</option>
              <option value="MOONSHOT">Moonshot AI (Kimi)</option>
              <option value="QWEN">通义千问</option>
            </select>
          </div>
          <div class="form-group">
            <label>扩展配置 (JSON)</label>
            <AutoResizeTextarea
              v-model="formData.extraConfig"
              placeholder='{"thinking": {"type": "enabled"}}'
              :min-rows="3"
              :max-rows="10"
              enter-behavior="newline"
              class="config-json-editor"
            />
          </div>
          <div class="quick-config-buttons">
            <button type="button" class="quick-btn" @click="applyQuickConfig('thinking')">+ 思考模式</button>
            <button type="button" class="quick-btn" @click="applyQuickConfig('usage')">+ 返回 Usage</button>
            <button type="button" class="quick-btn" @click="applyQuickConfig('filter')">+ 过滤思考内容</button>
          </div>
        </div>
      </transition>
    </div>

    <div class="form-actions">
      <button type="button" class="btn-cancel" @click="$emit('cancel')">取消</button>
      <button type="submit" class="btn-submit" :disabled="isSubmitting">
        {{ isSubmitting ? '保存中...' : '保存模型' }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useModelsStore } from '@/stores/models'
import { useToastStore } from '@/stores/toast'
import { useUserStore } from '@/stores/user'
import type { AIModel, AIModelCreateRequest, AIModelUpdateRequest } from '@/types'
import { AutoResizeTextarea } from '@/components'

const props = defineProps<{
  model: AIModel | null
  mode: 'create' | 'edit'
}>()

const emit = defineEmits<{
  submit: []
  cancel: []
}>()

const modelStore = useModelsStore()
const toastStore = useToastStore()
const userStore = useUserStore()
const isSubmitting = ref(false)
const showAdvancedConfig = ref(false)

const formData = reactive({
  name: '',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  maxTokens: 4096,
  temperature: 0.7,
  providerType: '',
  extraConfig: ''
})

const errors = reactive<Record<string, string>>({
  name: '',
  baseUrl: '',
  apiKey: '',
  modelName: ''
})

watch(
  () => props.model,
  (val) => {
    if (props.mode === 'edit' && val) {
      formData.name = val.name
      formData.baseUrl = val.baseUrl
      formData.apiKey = val.apiKey
      formData.modelName = val.modelName
      formData.maxTokens = val.maxTokens || 4096
      formData.temperature = val.temperature ?? 0.7
      formData.providerType = val.providerType || ''
      formData.extraConfig = val.extraConfig ? (typeof val.extraConfig === 'object' ? JSON.stringify(val.extraConfig, null, 2) : val.extraConfig) : ''
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

function resetForm() {
  formData.name = ''
  formData.baseUrl = ''
  formData.apiKey = ''
  formData.modelName = ''
  formData.maxTokens = 4096
  formData.temperature = 0.7
  formData.providerType = ''
  formData.extraConfig = ''
  showAdvancedConfig.value = false
  Object.keys(errors).forEach((key) => {
    errors[key] = ''
  })
}

function applyQuickConfig(type: string) {
  let base: Record<string, any> = {}
  try {
    if (formData.extraConfig.trim()) {
      base = JSON.parse(formData.extraConfig)
    }
  } catch { /* ignore */ }

  switch (type) {
    case 'thinking':
      base.thinking = { type: 'enabled' }
      break
    case 'usage':
      base.streamOptions = { includeUsage: true }
      break
    case 'filter':
      base.filterReasoningContent = true
      break
  }
  formData.extraConfig = JSON.stringify(base, null, 2)
  if (!showAdvancedConfig.value) {
    showAdvancedConfig.value = true
  }
}

function validate(): boolean {
  let valid = true
  Object.keys(errors).forEach((key) => {
    errors[key] = ''
  })

  if (!formData.name.trim()) {
    errors.name = '请输入模型名称'
    valid = false
  }

  if (!formData.baseUrl.trim()) {
    errors.baseUrl = '请输入 API 地址'
    valid = false
  }

  if (!formData.apiKey.trim()) {
    errors.apiKey = '请输入 API 密钥'
    valid = false
  }

  if (!formData.modelName.trim()) {
    errors.modelName = '请输入模型标识符'
    valid = false
  }

  if (!valid) {
    toastStore.error('请填写所有必填项')
  }

  return valid
}

async function handleSubmit() {
  if (!validate()) return

  const userId = userStore.user?.id
  if (!userId) {
    toastStore.error('用户信息不存在，请重新登录')
    return
  }

  isSubmitting.value = true
  try {
    if (props.mode === 'create') {
      const data: AIModelCreateRequest = {
        name: formData.name.trim(),
        baseUrl: formData.baseUrl.trim(),
        apiKey: formData.apiKey.trim(),
        modelName: formData.modelName.trim(),
        maxTokens: formData.maxTokens,
        temperature: formData.temperature,
        providerType: formData.providerType.trim() || undefined,
        extraConfig: formData.extraConfig.trim() || undefined
      }
      await modelStore.createModel(data, userId)
      toastStore.success('模型创建成功')
    } else {
      const data: AIModelUpdateRequest = {
        name: formData.name.trim(),
        baseUrl: formData.baseUrl.trim(),
        apiKey: formData.apiKey.trim(),
        modelName: formData.modelName.trim(),
        maxTokens: formData.maxTokens,
        temperature: formData.temperature,
        providerType: formData.providerType.trim() || undefined,
        extraConfig: formData.extraConfig.trim() || undefined
      }
      await modelStore.updateModel(props.model!.id, data, userId)
      toastStore.success('模型更新成功')
    }
    emit('submit')
  } catch (error: any) {
    const message = error.response?.data?.message || `${props.mode === 'create' ? '创建' : '更新'}失败，请稍后重试`
    toastStore.error(message)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.model-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-group.half {
  flex: 1;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #444;
}

.required {
  color: #d97706;
}

.form-group input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
  outline: none;
  background: #fafafa;
  color: #333;
  box-sizing: border-box;
}

.form-group input:focus {
  border-color: #1a1a1a;
  background: white;
}

.form-group input::placeholder {
  color: #aaa;
}

.form-group input.error {
  border-color: #ef4444;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

.btn-cancel {
  padding: 10px 20px;
  border: 1.5px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #555;
  background: white;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-cancel:hover {
  border-color: #ccc;
  background: #f8f8f8;
}

.btn-submit {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  background: #1a1a1a;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-submit:hover:not(:disabled) {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn-submit:active:not(:disabled) {
  transform: translateY(0);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.advanced-config {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.advanced-config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #555;
  background: #fafafa;
  user-select: none;
  transition: background 0.2s ease;
}

.advanced-config-header:hover {
  background: #f5f5f5;
}

.advanced-chevron {
  font-size: 10px;
  color: #999;
  transition: transform 0.2s ease;
  display: inline-block;
}

.advanced-chevron.expanded {
  transform: rotate(90deg);
}

.advanced-config-body {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-top: 1px solid #e8e8e8;
}

.config-json-editor :deep(.textarea-inner) {
  font-size: 13px;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
}

.form-group select {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  background: #fafafa;
  color: #333;
  outline: none;
  cursor: pointer;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-group select:focus {
  border-color: #1a1a1a;
  background: white;
}

.quick-config-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-btn {
  padding: 6px 12px;
  border: 1.5px dashed #d0d0d0;
  border-radius: 6px;
  font-size: 12px;
  color: #666;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-btn:hover {
  border-color: #1a1a1a;
  color: #1a1a1a;
  background: #faf9f7;
}

/* 折叠动画 */
.accordion-enter-active,
.accordion-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.accordion-enter-from,
.accordion-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.accordion-enter-to,
.accordion-leave-from {
  opacity: 1;
  max-height: 300px;
}
</style>
