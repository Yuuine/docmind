<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="localVisible" class="settings-modal-overlay" @click.self="handleClose">
        <div class="settings-modal">
          <div class="modal-header">
            <h2 class="modal-title">系统设置</h2>
            <button class="close-btn" @click="handleClose" aria-label="关闭" type="button">
              <Icon name="close" :size="20" />
            </button>
          </div>

          <div class="modal-body">
            <div class="settings-layout">
              <div class="settings-tabs">
                <button
                  v-for="tab in tabs"
                  :key="tab.id"
                  class="tab-item"
                  :class="{ active: activeTab === tab.id }"
                  @click="activeTab = tab.id"
                >
                  <Icon :name="tab.icon" :size="18" class="tab-icon" />
                  <span class="tab-label">{{ tab.label }}</span>
                </button>
              </div>

              <div class="settings-content">
                <div v-if="activeTab === 'general'" class="settings-panel">
                  <div class="panel-title">通用设置</div>
                  <div class="settings-description">
                    暂无通用设置项，敬请期待
                  </div>
                </div>

                <div v-else-if="activeTab === 'account'" class="settings-panel">
                  <div class="panel-title">账号管理</div>

                  <div class="settings-section">
                    <div class="form-group">
                      <label class="label">用户名</label>
                      <input
                        type="text"
                        :value="userStore.user?.username"
                        disabled
                        class="input"
                      />
                    </div>
                    <div class="form-group">
                      <label class="label">邮箱</label>
                      <input
                        v-model="form.email"
                        type="email"
                        placeholder="请输入邮箱"
                        class="input"
                      />
                    </div>
                    <div class="form-group">
                      <label class="label">手机号</label>
                      <input
                        v-model="form.phone"
                        type="tel"
                        placeholder="请输入手机号"
                        class="input"
                      />
                    </div>
                    <button
                      @click="handleSave"
                      :disabled="isSaving"
                      class="save-btn"
                    >
                      {{ isSaving ? '保存中...' : '保存' }}
                    </button>
                  </div>

                  <div class="settings-section">
                    <button @click="handleLogout" class="logout-btn">
                      <Icon name="logout" :size="16" />
                      <span>退出登录</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <ConfirmModal
    v-model="showLogoutConfirm"
    title="退出登录"
    message="确定要退出当前账号吗？"
    confirm-text="确定退出"
    cancel-text="取消"
    :is-destructive="true"
    @confirm="confirmLogout"
  />
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useToastStore } from '@/stores/toast'
import { userApi } from '@/api'
import { Icon } from '@/components/icons'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { getAxiosErrorMessage } from '@/utils/axiosMessage'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const userStore = useUserStore()
const toastStore = useToastStore()

const localVisible = ref(props.modelValue)
const activeTab = ref('account')
const isSaving = ref(false)
const showLogoutConfirm = ref(false)

const form = reactive({
  email: '',
  phone: ''
})

const tabs = [
  {
    id: 'general',
    label: '通用设置',
    icon: 'settings' as const
  },
  {
    id: 'account',
    label: '账号管理',
    icon: 'user' as const
  }
]

onMounted(async () => {
  await loadProfile()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    handleClose()
  }
}

watch(
  () => props.modelValue,
  async (newVal) => {
    localVisible.value = newVal
    if (newVal) {
      await loadProfile()
    }
  }
)

watch(localVisible, (newVal) => {
  emit('update:modelValue', newVal)
})

async function loadProfile() {
  try {
    const profile = await userApi.getProfile(userStore.user?.id)
    form.email = profile.email || ''
    form.phone = profile.phone || ''
  } catch (err) {
    console.error('加载用户资料失败', err)
  }
}

async function handleSave() {
  isSaving.value = true
  try {
    await userApi.updateProfile({
      email: form.email || undefined,
      phone: form.phone || undefined
    }, userStore.user?.id)
    toastStore.success('资料更新成功')
  } catch (err: unknown) {
    const msg = getAxiosErrorMessage(err, '保存失败')
    toastStore.error(msg)
  } finally {
    isSaving.value = false
  }
}

function handleLogout() {
  showLogoutConfirm.value = true
}

function confirmLogout() {
  handleClose()
  setTimeout(() => {
    userStore.logout()
    window.location.href = '/'
  }, 200)
}

function handleClose() {
  localVisible.value = false
}
</script>

<style scoped>
.settings-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.settings-modal {
  position: relative;
  background: #ffffff;
  border-radius: var(--radius-lg);
  width: 600px;
  max-width: 90vw;
  height: min(70vh, 520px);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-light);
}

.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-tertiary);
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.modal-body {
  flex: 1;
  overflow: hidden;
  padding: 0;
}

.settings-layout {
  display: flex;
  height: 100%;
  min-height: 400px;
}

.settings-tabs {
  width: 160px;
  padding: 16px 8px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
  color: var(--text-secondary);
}

.tab-item:hover {
  background: rgba(0, 0, 0, 0.04);
  color: var(--text-primary);
}

.tab-item.active {
  background: #ffffff;
  color: var(--color-accent);
  box-shadow: var(--shadow-sm);
}

.tab-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.tab-label {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.settings-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.settings-panel .panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.settings-description {
  padding: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 14px;
}

.settings-section {
  margin-bottom: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  font-size: 14px;
  outline: none;
  background: #ffffff;
  transition: all 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.input:not([disabled]):focus {
  background: #ffffff;
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.input[disabled] {
  background: var(--bg-secondary);
  color: var(--text-tertiary);
}

.save-btn {
  width: 100%;
  padding: 12px;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.save-btn:hover:not(:disabled) {
  background: #6d28d9;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  color: #ef4444;
  font-size: 14px;
  cursor: pointer;
  padding: 12px 16px;
  border: 1px solid rgba(254, 202, 202, 0.5);
  border-radius: var(--radius-sm);
  transition: all 0.2s;
  text-align: center;
  background: transparent;
  font-family: inherit;
  box-sizing: border-box;
}

.logout-btn:hover {
  background: #fef2f2;
  border-color: #fca5a5;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;
}

.modal-enter-active .settings-modal,
.modal-leave-active .settings-modal {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .settings-modal,
.modal-leave-to .settings-modal {
  transform: scale(0.95) translateY(-20px);
  opacity: 0;
}
</style>
