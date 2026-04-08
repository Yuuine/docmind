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
      <label>API 密钥 <span v-if="mode === 'create'" class="required">*</span>
        <span v-else class="optional">(选填，保持不变)</span>
      </label>
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

    <div class="advanced-settings">
      <button
        type="button"
        class="advanced-toggle"
        @click="showAdvanced = !showAdvanced"
      >
        <span class="advanced-icon" :class="{ 'is-open': showAdvanced }">▶</span>
        <span>高级设置</span>
        <span v-if="formData.extraConfig.trim()" class="advanced-badge">已配置</span>
      </button>
      
      <div v-show="showAdvanced" class="advanced-content">
        <JsonEditor
          v-model="formData.extraConfig"
          :model-name="formData.modelName"
          :temperature="formData.temperature"
          :max-tokens="formData.maxTokens"
          ref="jsonEditorRef"
        />
      </div>
    </div>

    <div class="form-actions">
      <button type="button" class="btn-cancel" @click="$emit('cancel')">取消</button>
      <button type="submit" class="btn-submit" :disabled="isSubmitting || isTesting">
        {{ isTesting ? '测试连接中...' : (isSubmitting ? '保存中...' : '保存模型') }}
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
import { getAxiosErrorMessage } from '@/utils/axiosMessage'
import { modelApi } from '@/api'
import JsonEditor from '@/components/JsonEditor.vue'

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
const isTesting = ref(false)

const formData = reactive({
  name: '',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  maxTokens: 4096,
  temperature: 0.7,
  extraConfig: ''
})

const jsonEditorRef = ref<InstanceType<typeof JsonEditor>>()
const showAdvanced = ref(false)

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
      formData.apiKey = ''
      formData.modelName = val.modelName
      formData.maxTokens = val.maxTokens || 4096
      formData.temperature = val.temperature ?? 0.7
      formData.extraConfig = val.extraConfig || ''
      showAdvanced.value = !!val.extraConfig?.trim()
    } else {
      resetForm()
      showAdvanced.value = false
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
  formData.extraConfig = ''
  Object.keys(errors).forEach((key) => {
    errors[key] = ''
  })
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

  if (props.mode === 'create' && !formData.apiKey.trim()) {
    errors.apiKey = '请输入 API 密钥'
    valid = false
  }

  if (!formData.modelName.trim()) {
    errors.modelName = '请输入模型标识符'
    valid = false
  }

  if (formData.extraConfig.trim()) {
    try {
      JSON.parse(formData.extraConfig)
    } catch {
      toastStore.error('自定义配置JSON格式错误')
      valid = false
    }
  }

  if (!valid && Object.values(errors).some(e => e)) {
    toastStore.error('请填写所有必填项')
  }

  return valid
}

async function testConnection(): Promise<boolean> {
  if (props.mode === 'edit' && !formData.apiKey.trim()) {
    return true
  }

  isTesting.value = true
  try {
    await modelApi.testConnection({
      baseUrl: formData.baseUrl.trim(),
      apiKey: formData.apiKey.trim(),
      modelName: formData.modelName.trim()
    })
    return true
  } catch {
    return false
  } finally {
    isTesting.value = false
  }
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
    const connected = await testConnection()
    if (!connected) {
      toastStore.error('连接失败')
      return
    }

    const extraConfig = formData.extraConfig.trim()

    if (props.mode === 'create') {
      const data: AIModelCreateRequest = {
        name: formData.name.trim(),
        baseUrl: formData.baseUrl.trim(),
        apiKey: formData.apiKey.trim(),
        modelName: formData.modelName.trim(),
        maxTokens: formData.maxTokens,
        temperature: formData.temperature,
        extraConfig
      }
      await modelStore.createModel(data, userId)
      toastStore.success('模型创建成功')
    } else {
      const data: AIModelUpdateRequest = {
        name: formData.name.trim(),
        baseUrl: formData.baseUrl.trim(),
        apiKey: formData.apiKey.trim() || undefined,
        modelName: formData.modelName.trim(),
        maxTokens: formData.maxTokens,
        temperature: formData.temperature,
        extraConfig
      }
      await modelStore.updateModel(props.model!.id, data, userId)
      toastStore.success('模型更新成功')
    }
    emit('submit')
  } catch (error: unknown) {
    const fallback = `${props.mode === 'create' ? '创建' : '更新'}失败，请稍后重试`
    toastStore.error(getAxiosErrorMessage(error, fallback))
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

.optional {
  color: #999;
  font-weight: 400;
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
  border-color: var(--color-accent);
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
  color: var(--btn-primary-text);
  background: var(--btn-primary-bg);
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-submit:hover:not(:disabled) {
  background: var(--btn-primary-bg-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--btn-primary-bg) 30%, transparent);
}

.btn-submit:active:not(:disabled) {
  transform: translateY(0);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.advanced-settings {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.advanced-toggle {
  width: 100%;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8f9fa;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #555;
  transition: background 0.2s ease;
}

.advanced-toggle:hover {
  background: #f0f1f2;
}

.advanced-icon {
  font-size: 10px;
  transition: transform 0.2s ease;
}

.advanced-icon.is-open {
  transform: rotate(90deg);
}

.advanced-badge {
  margin-left: auto;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: #16a34a;
  background: #dcfce7;
  border-radius: 12px;
}

.advanced-content {
  padding: 16px;
  background: white;
}
</style>
