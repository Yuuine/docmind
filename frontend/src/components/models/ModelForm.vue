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

const formData = reactive({
  name: '',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  maxTokens: 4096,
  temperature: 0.7
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
        temperature: formData.temperature
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
        temperature: formData.temperature
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
</style>
