<template>
  <div class="login-register-page">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="nav-brand" @click="goToHome">
        <div class="logo">
          <Icon name="logo" :size="28" />
        </div>
        <span class="brand-text">DocMind</span>
      </div>
    </nav>

    <div class="container">
      <div class="card" :class="{ shake: isShaking }">
        <div class="header">
          <h1>{{ isLoginMode ? '欢迎回来' : '创建账号' }}</h1>
          <p>{{ isLoginMode ? '登录以继续您的文档探索之旅' : '开始您的智能文档检索体验' }}</p>
        </div>

        <div class="tabs">
          <button :class="{ active: isLoginMode }" @click="switchToLogin">登录</button>
          <button :class="{ active: !isLoginMode }" @click="switchToRegister">注册</button>
        </div>

        <div class="form-wrapper">
          <transition name="form-switch" mode="out-in">
            <form v-if="isLoginMode" key="login" @submit.prevent="handleLogin" class="form">
              <div class="form-group">
                <label>登录标识</label>
                <div class="input-wrapper">
                  <Icon name="user" :size="16" class="input-icon" />
                  <input v-model="loginForm.username" type="text" placeholder="用户名/邮箱/手机号" />
                </div>
              </div>

              <div class="form-group">
                <label>密码</label>
                <div class="input-wrapper">
                  <Icon name="lock" :size="16" class="input-icon" />
                  <input v-model="loginForm.password" :type="showLoginPassword ? 'text' : 'password'" placeholder="请输入密码" />
                  <button type="button" @click="showLoginPassword = !showLoginPassword" class="eye-btn">
                    <Icon :name="showLoginPassword ? 'eyeOff' : 'eye'" :size="16" />
                  </button>
                </div>
              </div>

              <button type="submit" :disabled="isLoading" class="submit-btn">
                {{ isLoading ? '登录中...' : '登录' }}
              </button>
            </form>

            <form v-else key="register" @submit.prevent="handleRegister" class="form">
              <div class="form-group">
                <label>用户名 <span class="required">*</span></label>
                <div class="input-wrapper">
                  <Icon name="user" :size="16" class="input-icon" />
                  <input v-model="registerForm.username" type="text" placeholder="请输入用户名" />
                </div>
              </div>

              <div class="form-row">
                <div class="form-group half">
                  <label>邮箱</label>
                  <div class="input-wrapper">
                    <Icon name="mail" :size="16" class="input-icon" />
                    <input v-model="registerForm.email" type="email" placeholder="选填" />
                  </div>
                </div>

                <div class="form-group half">
                  <label>手机号</label>
                  <div class="input-wrapper">
                    <Icon name="phone" :size="16" class="input-icon" />
                    <input v-model="registerForm.phone" type="tel" placeholder="选填" />
                  </div>
                </div>
              </div>

              <p class="hint">邮箱和手机号至少填写一项</p>

              <div class="form-row">
                <div class="form-group half">
                  <label>密码 <span class="required">*</span></label>
                  <div class="input-wrapper">
                    <Icon name="lock" :size="16" class="input-icon" />
                    <input v-model="registerForm.password" :type="showRegisterPassword ? 'text' : 'password'" placeholder="请输入密码" />
                    <button type="button" @click="showRegisterPassword = !showRegisterPassword" class="eye-btn">
                      <Icon :name="showRegisterPassword ? 'eyeOff' : 'eye'" :size="16" />
                    </button>
                  </div>
                </div>

                <div class="form-group half">
                  <label>确认密码 <span class="required">*</span></label>
                  <div class="input-wrapper">
                    <Icon name="lock" :size="16" class="input-icon" />
                    <input v-model="registerForm.confirmPassword" :type="showRegisterConfirmPassword ? 'text' : 'password'" placeholder="请再次输入密码" />
                    <button type="button" @click="showRegisterConfirmPassword = !showRegisterConfirmPassword" class="eye-btn">
                      <Icon :name="showRegisterConfirmPassword ? 'eyeOff' : 'eye'" :size="16" />
                    </button>
                  </div>
                </div>
              </div>

              <button type="submit" :disabled="isLoading" class="submit-btn">
                {{ isLoading ? '注册中...' : '创建账号' }}
              </button>
            </form>
          </transition>
        </div>
      </div>
    </div>

    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-grid"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToastStore } from '@/stores/toast'
import { Icon } from '@/components/icons'

const router = useRouter()
const userStore = useUserStore()
const toastStore = useToastStore()

const isLoginMode = ref(true)
const isLoading = ref(false)
const isShaking = ref(false)
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const showRegisterConfirmPassword = ref(false)

const triggerShake = () => {
  isShaking.value = true
  setTimeout(() => {
    isShaking.value = false
  }, 500)
}

const loginForm = reactive({
  username: '',
  password: ''
})

const loginErrors = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const registerErrors = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const goToHome = () => {
  router.push('/')
}

const switchToLogin = () => {
  isLoginMode.value = true
  clearErrors()
}

const switchToRegister = () => {
  isLoginMode.value = false
  clearErrors()
}

const clearErrors = () => {
  loginErrors.username = ''
  loginErrors.password = ''
  registerErrors.username = ''
  registerErrors.email = ''
  registerErrors.phone = ''
  registerErrors.password = ''
  registerErrors.confirmPassword = ''
}

const validateLoginForm = (): boolean => {
  let valid = true
  loginErrors.username = ''
  loginErrors.password = ''

  if (!loginForm.username.trim()) {
    toastStore.error('请输入登录标识')
    loginErrors.username = '请输入登录标识'
    valid = false
  }

  if (!loginForm.password) {
    toastStore.error('请输入密码')
    loginErrors.password = '请输入密码'
    valid = false
  }

  if (!valid) {
    triggerShake()
  }

  return valid
}

const validateRegisterForm = (): boolean => {
  let valid = true
  Object.keys(registerErrors).forEach(key => {
    registerErrors[key as keyof typeof registerErrors] = ''
  })

  if (!registerForm.username.trim()) {
    toastStore.error('请输入用户名')
    registerErrors.username = '请输入用户名'
    valid = false
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  const phoneRegex = /^1[3-9]\d{9}$/

  if (registerForm.email.trim() && !emailRegex.test(registerForm.email)) {
    toastStore.error('邮箱格式不正确')
    registerErrors.email = '邮箱格式不正确'
    valid = false
  }

  if (registerForm.phone.trim()) {
    if (!/^\d+$/.test(registerForm.phone)) {
      toastStore.error('手机号只能包含数字')
      registerErrors.phone = '手机号只能包含数字'
      valid = false
    } else if (!phoneRegex.test(registerForm.phone)) {
      toastStore.error('请输入正确的11位手机号')
      registerErrors.phone = '请输入正确的11位手机号'
      valid = false
    }
  }

  if (!registerForm.email.trim() && !registerForm.phone.trim()) {
    toastStore.error('邮箱和手机号至少填写一项')
    registerErrors.email = '邮箱和手机号至少填写一项'
    valid = false
  }

  if (!registerForm.password) {
    toastStore.error('请输入密码')
    registerErrors.password = '请输入密码'
    valid = false
  } else if (registerForm.password.length < 6) {
    toastStore.error('密码至少6位')
    registerErrors.password = '密码至少6位'
    valid = false
  }

  if (!registerForm.confirmPassword) {
    toastStore.error('请确认密码')
    registerErrors.confirmPassword = '请确认密码'
    valid = false
  } else if (registerForm.password !== registerForm.confirmPassword) {
    toastStore.error('两次输入的密码不一致')
    registerErrors.confirmPassword = '两次输入的密码不一致'
    valid = false
  }

  if (!valid) {
    triggerShake()
  }

  return valid
}

const handleLogin = async () => {
  if (!validateLoginForm()) return

  isLoading.value = true

  try {
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    })
    toastStore.success('登录成功')
    router.push('/chat')
  } catch (error: any) {
    const message = error.response?.data?.message || '登录失败，请稍后重试'
    toastStore.error(message, 4000)
    triggerShake()
  } finally {
    isLoading.value = false
  }
}

const handleRegister = async () => {
  if (!validateRegisterForm()) return

  isLoading.value = true

  try {
    await userStore.register({
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email || undefined,
      phone: registerForm.phone || undefined
    })
    toastStore.success('注册成功')
    router.push('/chat')
  } catch (error: any) {
    const message = error.response?.data?.message || '注册失败，请稍后重试'
    toastStore.error(message, 4000)
    triggerShake()
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-register-page {
  min-height: 100vh;
  background: #faf9f7;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* 导航栏 */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 48px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.nav-brand:hover {
  opacity: 0.7;
}

.logo {
  width: 28px;
  height: 28px;
  color: #1a1a1a;
}

.brand-text {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.02em;
}

/* 主容器 */
.container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 24px 40px;
  position: relative;
  z-index: 10;
}

/* 卡片 */
.card {
  background: white;
  border-radius: 16px;
  padding: 36px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
}

.header {
  text-align: center;
  margin-bottom: 24px;
}

.header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.header p {
  font-size: 14px;
  color: #888;
  font-weight: 400;
}

/* 标签页 */
.tabs {
  display: flex;
  margin-bottom: 24px;
  background: #f8f8f8;
  border-radius: 10px;
  padding: 4px;
}

.tabs button {
  flex: 1;
  padding: 10px 16px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: #888;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.25s ease;
}

.tabs button:hover {
  color: #555;
}

.tabs button.active {
  background: white;
  color: #1a1a1a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 表单容器 */
.form-wrapper {
  position: relative;
  overflow: hidden;
}

/* 表单 */
.form {
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

.form-group .required {
  color: #d97706;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  color: #999;
  pointer-events: none;
}

.form-group input {
  width: 100%;
  padding: 10px 14px 10px 38px;
  border: 1.5px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
  outline: none;
  background: #fafafa;
  color: #333;
}

.form-group input:focus {
  border-color: #1a1a1a;
  background: white;
}

.form-group input::placeholder {
  color: #aaa;
}

.password-input {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input input {
  padding-right: 40px;
}

.eye-btn {
  position: absolute;
  right: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 4px;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.eye-btn:hover {
  color: #666;
}

.hint {
  font-size: 11px;
  color: #999;
  margin-top: -10px;
  margin-bottom: -4px;
  font-weight: 400;
}

/* 提交按钮 */
.submit-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  background: #1a1a1a;
  cursor: pointer;
  transition: all 0.25s ease;
  margin-top: 4px;
}

.submit-btn:hover:not(:disabled) {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 卡片抖动动画 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-6px); }
  20%, 40%, 60%, 80% { transform: translateX(6px); }
}

.card.shake {
  animation: shake 0.5s ease-in-out;
}

/* 表单切换动画 */
.form-switch-enter-active,
.form-switch-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.form-switch-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.form-switch-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 隐藏浏览器原生的密码显示/隐藏按钮 */
.form-group input[type="password"]::-ms-reveal {
  display: none;
}

.form-group input[type="password"]::-ms-clear {
  display: none;
}

.form-group input[type="password"]::-webkit-credentials-auto-fill-button {
  visibility: hidden;
  pointer-events: none;
  position: absolute;
  right: 0;
}

/* 背景装饰 */
.bg-decoration {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1;
}

.bg-grid {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 800px;
  height: 600px;
  background-image:
    linear-gradient(rgba(0,0,0,0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,0,0,0.06) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at center, black 0%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse at center, black 0%, transparent 70%);
}

/* 响应式 */
@media (max-width: 480px) {
  .navbar {
    padding: 16px 20px;
  }

  .card {
    padding: 28px 20px;
  }

  .header h1 {
    font-size: 24px;
  }

  .form-row {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
