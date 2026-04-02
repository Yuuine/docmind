<template>
  <div class="login-register-page">
    <div class="background"></div>
    
    <div class="container">
      <div class="card">
        <div class="header">
          <h1>DocMind RAG</h1>
          <p>智能文档检索与问答系统</p>
        </div>
        
        <div class="tabs">
          <button :class="{ active: isLoginMode }" @click="switchToLogin">登录</button>
          <button :class="{ active: !isLoginMode }" @click="switchToRegister">注册</button>
        </div>
        
        <form v-if="isLoginMode" @submit.prevent="handleLogin" class="form">
          <div class="form-group">
            <label>登录标识</label>
            <input v-model="loginForm.username" type="text" placeholder="用户名/邮箱/手机号" />
            <span v-if="loginErrors.username" class="error">{{ loginErrors.username }}</span>
          </div>
          
          <div class="form-group">
            <label>密码</label>
            <div class="password-input">
              <input v-model="loginForm.password" :type="showLoginPassword ? 'text' : 'password'" placeholder="请输入密码" />
              <button type="button" @click="showLoginPassword = !showLoginPassword">
                {{ showLoginPassword ? '👁️' : '🙈' }}
              </button>
            </div>
            <span v-if="loginErrors.password" class="error">{{ loginErrors.password }}</span>
          </div>
          
          <div v-if="loginError" class="form-error">{{ loginError }}</div>
          
          <button type="submit" :disabled="isLoading" class="submit-btn">
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
        </form>
        
        <form v-else @submit.prevent="handleRegister" class="form">
          <div class="form-group">
            <label>用户名 <span class="required">*</span></label>
            <input v-model="registerForm.username" type="text" placeholder="请输入用户名" />
            <span v-if="registerErrors.username" class="error">{{ registerErrors.username }}</span>
          </div>
          
          <div class="form-group">
            <label>邮箱</label>
            <input v-model="registerForm.email" type="email" placeholder="请输入邮箱（选填）" />
            <span v-if="registerErrors.email" class="error">{{ registerErrors.email }}</span>
          </div>
          
          <div class="form-group">
            <label>手机号</label>
            <input v-model="registerForm.phone" type="tel" placeholder="请输入手机号（选填）" />
            <span v-if="registerErrors.phone" class="error">{{ registerErrors.phone }}</span>
          </div>
          
          <div class="form-group">
            <label>密码 <span class="required">*</span></label>
            <div class="password-input">
              <input v-model="registerForm.password" :type="showRegisterPassword ? 'text' : 'password'" placeholder="请输入密码" />
              <button type="button" @click="showRegisterPassword = !showRegisterPassword">
                {{ showRegisterPassword ? '👁️' : '🙈' }}
              </button>
            </div>
            <span v-if="registerErrors.password" class="error">{{ registerErrors.password }}</span>
          </div>
          
          <div class="form-group">
            <label>确认密码 <span class="required">*</span></label>
            <div class="password-input">
              <input v-model="registerForm.confirmPassword" :type="showRegisterConfirmPassword ? 'text' : 'password'" placeholder="请再次输入密码" />
              <button type="button" @click="showRegisterConfirmPassword = !showRegisterConfirmPassword">
                {{ showRegisterConfirmPassword ? '👁️' : '🙈' }}
              </button>
            </div>
            <span v-if="registerErrors.confirmPassword" class="error">{{ registerErrors.confirmPassword }}</span>
          </div>
          
          <p class="hint">邮箱和手机号至少填写一项</p>
          
          <div v-if="registerError" class="form-error">{{ registerError }}</div>
          
          <button type="submit" :disabled="isLoading" class="submit-btn">
            {{ isLoading ? '注册中...' : '注册' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const isLoginMode = ref(true)

const isLoading = ref(false)

const loginError = ref('')
const registerError = ref('')

const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const showRegisterConfirmPassword = ref(false)

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

const switchToLogin = () => {
  isLoginMode.value = true
  loginError.value = ''
  registerError.value = ''
}

const switchToRegister = () => {
  isLoginMode.value = false
  loginError.value = ''
  registerError.value = ''
}

const validateLoginForm = () => {
  let valid = true
  loginErrors.username = ''
  loginErrors.password = ''
  
  if (!loginForm.username.trim()) {
    loginErrors.username = '请输入登录标识'
    valid = false
  }
  
  if (!loginForm.password) {
    loginErrors.password = '请输入密码'
    valid = false
  }
  
  return valid
}

const validateRegisterForm = () => {
  let valid = true
  Object.keys(registerErrors).forEach(key => {
    registerErrors[key as keyof typeof registerErrors] = ''
  })
  
  if (!registerForm.username.trim()) {
    registerErrors.username = '请输入用户名'
    valid = false
  }
  
  if (!registerForm.email.trim() && !registerForm.phone.trim()) {
    registerErrors.email = '邮箱和手机号至少填写一项'
    valid = false
  }
  
  if (!registerForm.password) {
    registerErrors.password = '请输入密码'
    valid = false
  } else if (registerForm.password.length < 6) {
    registerErrors.password = '密码至少6位'
    valid = false
  }
  
  if (!registerForm.confirmPassword) {
    registerErrors.confirmPassword = '请确认密码'
    valid = false
  } else if (registerForm.password !== registerForm.confirmPassword) {
    registerErrors.confirmPassword = '两次输入的密码不一致'
    valid = false
  }
  
  return valid
}

const handleLogin = async () => {
  if (!validateLoginForm()) return
  
  isLoading.value = true
  loginError.value = ''
  
  try {
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    })
    router.push('/')
  } catch (error: any) {
    loginError.value = error.response?.data?.message || '登录失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

const handleRegister = async () => {
  if (!validateRegisterForm()) return
  
  isLoading.value = true
  registerError.value = ''
  
  try {
    await userStore.register({
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email || undefined,
      phone: registerForm.phone || undefined
    })
    router.push('/')
  } catch (error: any) {
    registerError.value = error.response?.data?.message || '注册失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 50%, rgba(255,255,255,0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(255,255,255,0.1) 0%, transparent 50%);
}

.container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 440px;
  padding: 20px;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1a202c;
  margin-bottom: 8px;
}

.header p {
  font-size: 14px;
  color: #718096;
}

.tabs {
  display: flex;
  margin-bottom: 32px;
  background: #f7fafc;
  border-radius: 8px;
  padding: 4px;
}

.tabs button {
  flex: 1;
  padding: 12px 16px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.tabs button:hover {
  color: #2d3748;
}

.tabs button.active {
  background: white;
  color: #667eea;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #2d3748;
}

.form-group .required {
  color: #e53e3e;
}

.form-group input {
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s ease;
  outline: none;
}

.form-group input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.password-input {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input input {
  flex: 1;
  padding-right: 48px;
}

.password-input button {
  position: absolute;
  right: 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
  padding: 4px;
}

.error {
  font-size: 12px;
  color: #e53e3e;
}

.hint {
  font-size: 12px;
  color: #718096;
  margin-top: -12px;
}

.form-error {
  padding: 12px;
  background: #fff5f5;
  border: 1px solid #feb2b2;
  border-radius: 8px;
  color: #c53030;
  font-size: 14px;
}

.submit-btn {
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .card {
    padding: 28px 20px;
  }
  
  .header h1 {
    font-size: 24px;
  }
}
</style>
