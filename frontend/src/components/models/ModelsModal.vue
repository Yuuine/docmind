<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="closeModal">
        <div class="modal-container">
          <div class="modal-header">
            <h2>模型管理</h2>
            <button class="close-btn" @click="closeModal">
              <Icon name="close" :size="18" />
            </button>
          </div>

          <div class="modal-body">
            <template v-if="currentView === 'list'">
              <div v-if="modelStore.models.length === 0" class="empty-state">
                <Icon name="settings" :size="40" color="#ccc" />
                <p>暂无模型配置</p>
                <span>点击下方按钮添加你的第一个 AI 模型</span>
              </div>

              <div v-else class="model-list">
                <div
                  v-for="m in modelStore.models"
                  :key="m.id"
                  class="model-card"
                >
                  <div class="model-info">
                    <div class="model-name-row">
                      <span class="model-name">{{ m.name }}</span>
                      <span v-if="m.isActive" class="active-tag">当前使用</span>
                    </div>
                    <span class="model-url">{{ m.baseUrl }}</span>
                  </div>
                  <div class="model-actions">
                    <button
                      class="action-btn edit"
                      title="编辑"
                      @click="startEdit(m)"
                    >
                      <Icon name="edit" :size="15" />
                    </button>
                    <button
                      class="action-btn delete"
                      title="删除"
                      @click="handleDelete(m)"
                    >
                      <Icon name="trash" :size="15" />
                    </button>
                  </div>
                </div>
              </div>

              <button class="add-model-btn" @click="startCreate">
                <Icon name="plus" :size="16" />
                添加新模型
              </button>
            </template>

            <template v-else>
              <ModelForm
                :model="editingModel"
                :mode="formMode"
                @submit="onFormSubmit"
                @cancel="backToList"
              />
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <ConfirmModal
    v-model="showDeleteModelModal"
    title="删除模型"
    :message="`确定要删除模型「${targetDeleteModel?.name || ''}」吗？此操作不可撤销。`"
    confirm-text="确定删除"
    cancel-text="取消"
    :is-destructive="true"
    @confirm="executeDeleteModel"
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useModelsStore } from '@/stores/models'
import { useToastStore } from '@/stores/toast'
import { useUserStore } from '@/stores/user'
import { Icon } from '@/components/icons'
import ModelForm from './ModelForm.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import type { AIModel } from '@/types'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const modelStore = useModelsStore()
const toastStore = useToastStore()
const userStore = useUserStore()

type ViewMode = 'list' | 'form'
const currentView = ref<ViewMode>('list')
const editingModel = ref<AIModel | null>(null)
const formMode = ref<'create' | 'edit'>('create')
const showDeleteModelModal = ref(false)
const targetDeleteModel = ref<AIModel | null>(null)

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      currentView.value = 'list'
      const userId = userStore.user?.id
      if (userId) {
        modelStore.loadModels(userId)
      }
    }
  }
)

function closeModal() {
  emit('update:modelValue', false)
}

function startCreate() {
  editingModel.value = null
  formMode.value = 'create'
  currentView.value = 'form'
}

function startEdit(model: AIModel) {
  editingModel.value = model
  formMode.value = 'edit'
  currentView.value = 'form'
}

function backToList() {
  currentView.value = 'list'
  editingModel.value = null
}

async function onFormSubmit() {
  backToList()
}

function handleDelete(model: AIModel) {
  targetDeleteModel.value = model
  showDeleteModelModal.value = true
}

async function executeDeleteModel() {
  if (!targetDeleteModel.value) return

  const userId = userStore.user?.id
  if (!userId) {
    toastStore.error('用户信息不存在，请重新登录')
    return
  }

  try {
    await modelStore.deleteModel(targetDeleteModel.value.id, userId)
    toastStore.success('模型已删除')
  } catch (error: any) {
    const message = error.response?.data?.message || '删除失败，请稍后重试'
    toastStore.error(message)
  } finally {
    targetDeleteModel.value = null
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
}

.modal-container {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 520px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.02em;
  margin: 0;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  color: #999;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #555;
}

.modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px 32px;
  gap: 12px;
}

.empty-state p {
  font-size: 15px;
  font-weight: 500;
  color: #666;
  margin: 0;
}

.empty-state span {
  font-size: 13px;
  color: #aaa;
}

/* 模型列表 */
.model-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.model-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: white;
  border: 1.5px solid #eee;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.model-card:hover {
  border-color: #ddd;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.model-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.model-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.01em;
}

.active-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: #166534;
  background: #dcfce7;
  border-radius: 6px;
  white-space: nowrap;
}

.model-url {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
}

/* 操作按钮 */
.model-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn.edit {
  color: #666;
  background: transparent;
}

.action-btn.edit:hover {
  background: #f0f0f0;
  color: #333;
}

.action-btn.delete {
  color: #999;
  background: transparent;
}

.action-btn.delete:hover {
  background: #fef2f2;
  color: #ef4444;
}

/* 添加按钮 */
.add-model-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 12px;
  border: 1.5px dashed #d9d9d9;
  border-radius: 10px;
  background: white;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.25s ease;
}

.add-model-btn:hover {
  border-color: #bbb;
  color: #333;
  background:fafafa;
}

/* 过渡动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-active .modal-container,
.modal-fade-leave-active .modal-container {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-container {
  transform: scale(0.96) translateY(8px);
  opacity: 0;
}

.modal-fade-leave-to .modal-container {
  transform: scale(0.98);
  opacity: 0;
}

@media (max-width: 480px) {
  .modal-container {
    width: 95%;
    max-height: 85vh;
    border-radius: 14px;
  }

  .modal-header {
    padding: 16px 18px 14px;
  }

  .modal-body {
    padding: 16px 18px;
  }

  .model-card {
    padding: 12px 14px;
  }

  .model-url {
    max-width: 180px;
  }
}
</style>
