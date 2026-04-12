<template>
  <div class="settings-page">
    <h1 class="page-title">设置</h1>

    <div class="card">
      <h2 class="card-title">个人资料</h2>
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

    <div class="card">
      <h2 class="card-title">账号</h2>
      <button @click="showLogoutConfirm = true" class="logout-btn">退出登录</button>
    </div>

    <ConfirmModal
      v-model="showLogoutConfirm"
      title="退出登录"
      message="确定要退出当前账号吗？退出后需要重新登录才能继续使用。"
      confirm-text="确定退出"
      cancel-text="取消"
      :is-destructive="true"
      @confirm="handleLogout"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useToastStore } from '@/stores/toast'
import { userApi } from '@/api'
import { Icon } from '@/components/icons'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { getAxiosErrorMessage } from '@/utils/axiosMessage'

const userStore = useUserStore()
const toastStore = useToastStore()

const form = reactive({
  email: '',
  phone: ''
})
const isSaving = ref(false)
const showLogoutConfirm = ref(false)

onMounted(async () => {
  await loadProfile()
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
  userStore.logout()
  window.location.href = '/'
}
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  background: transparent;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 24px;
  align-self: flex-start;
  max-width: 640px;
  width: 100%;
}

.card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  -webkit-backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  max-width: 640px;
  width: 100%;
  box-sizing: border-box;
  box-shadow: var(--shadow-glass-sm);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 20px;
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
  color: #444;
}

.input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid var(--glass-border-subtle);
  border-radius: var(--radius-sm);
  font-size: 14px;
  outline: none;
  background: var(--glass-bg-light);
  transition: all var(--transition-fast);
  box-sizing: border-box;
  font-family: inherit;
}

input:not([disabled]) {
  background: var(--glass-bg-light);
  border: 1.5px solid var(--glass-border-subtle);
  backdrop-filter: blur(var(--blur-sm));
  -webkit-backdrop-filter: blur(var(--blur-sm));
  transition: all var(--transition-fast);
}

input:not([disabled]):focus {
  background: rgba(255, 255, 255, 0.75);
  border-color: var(--glass-border);
  box-shadow: 0 0 0 3px rgba(26, 26, 26, 0.04);
}

input[disabled] {
  background: rgba(245, 245, 245, 0.6);
}

.save-btn {
  width: 100%;
  padding: 12px;
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), background-color var(--transition-fast);
  font-family: inherit;
}

.save-btn:hover:not(:disabled) {
  background: var(--btn-primary-bg-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--btn-primary-bg) 30%, transparent);
}

.save-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.logout-btn {
  width: 100%;
  color: #ef4444;
  font-size: 14px;
  cursor: pointer;
  padding: 12px 16px;
  border: 1px solid rgba(254, 202, 202, 0.5);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);
  text-align: center;
  background: transparent;
  font-family: inherit;
  box-sizing: border-box;
}

.logout-btn:hover {
  background: rgba(254, 226, 226, 0.7);
  border-color: #fca5a5;
  transform: translateY(-1px);
  box-shadow: var(--shadow-glass-sm);
}

@media (max-width: 768px) {
  .settings-page {
    padding: 20px 16px;
  }

  .page-title {
    font-size: 20px;
  }
}
</style>
