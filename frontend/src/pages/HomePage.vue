<template>
  <div class="home-page">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="nav-brand">
        <div class="logo">
          <Icon name="logo" :size="32" />
        </div>
        <span class="brand-text">DocMind</span>
      </div>
      <div class="nav-actions">
        <template v-if="userStore.isLoggedIn">
          <span class="username">{{ userStore.user?.username }}</span>
          <button @click="handleLogout" class="btn btn-secondary">退出</button>
        </template>
        <template v-else>
          <button @click="goToLogin" class="btn btn-primary">登录</button>
        </template>
      </div>
    </nav>

    <!-- 主内容区 - Hero 单独一屏 -->
    <main class="main-content">
      <div class="center-container">
        <!-- Hero 区域 -->
        <section class="hero">
          <h1 class="hero-title">
            智能文档检索
            <br />
            <span class="accent">让知识触手可及</span>
          </h1>
          <p class="hero-description">
            基于 RAG 技术的企业级文档问答系统。
            <br />
            支持多种格式，精准语义检索，自然语言交互。
          </p>

          <!-- 主要操作区 -->
          <div class="hero-actions">
            <button v-if="!userStore.isLoggedIn" @click="goToLogin" class="btn btn-large btn-primary">
              开始使用
            </button>
            <button v-else @click="goToChat" class="btn btn-large btn-primary">
              进入对话
            </button>
          </div>

          <!-- 次要链接 -->
          <div v-if="userStore.isLoggedIn" class="hero-links">
            <router-link to="/documents" class="link">
              查看我的文档
            </router-link>
          </div>
        </section>
      </div>
    </main>

    <!-- 功能特性 - 在 Hero 下方，需要滚动查看 -->
    <section class="features-section">
      <div class="features-container">
        <div class="features">
          <div class="feature-item">
            <div class="feature-icon">
              <Icon name="document" :size="28" stroke-width="1.5" />
            </div>
            <span>多格式支持</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <Icon name="search" :size="28" stroke-width="1.5" />
            </div>
            <span>语义检索</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <Icon name="message" :size="28" stroke-width="1.5" />
            </div>
            <span>智能问答</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="footer">
      <p>© 2024 DocMind RAG</p>
    </footer>

    <!-- 背景装饰 - 边缘淡化 -->
    <div class="bg-decoration">
      <div class="bg-grid"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Icon } from '@/components/icons'

const router = useRouter()
const userStore = useUserStore()

const goToLogin = () => {
  router.push('/login')
}

const goToChat = () => {
  router.push('/chat')
}

const handleLogout = () => {
  userStore.logout()
}
</script>

<style scoped>
/* 引入优雅字体 */
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;500;600;700&family=Inter:wght@300;400;500;600&display=swap');

.home-page {
  min-height: 100vh;
  background: transparent;
  display: flex;
  flex-direction: column;
  position: relative;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* 导航栏 - 极简风格 */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 48px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 32px;
  height: 32px;
  color: #1a1a1a;
}

.logo svg {
  width: 100%;
  height: 100%;
}

.brand-text {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.02em;
  font-family: 'Inter', sans-serif;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.username {
  font-size: 15px;
  color: #666;
  font-weight: 400;
}

/* 按钮样式 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  text-decoration: none;
  font-family: 'Inter', sans-serif;
}

.btn-primary {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
}

.btn-primary:hover {
  background: var(--btn-primary-bg-hover);
  transform: translateY(-2px);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--btn-primary-bg) 35%, transparent);
}

.btn-secondary {
  background: transparent;
  color: #666;
  border: 1px solid #e0e0e0;
}

.btn-secondary:hover {
  background: #f5f5f5;
  border-color: #ccc;
}

.btn-large {
  padding: 18px 56px;
  font-size: 16px;
  border-radius: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

/* 主内容 - 占满一屏，增加留白 */
.main-content {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 10;
  padding: 100px 40px 60px;
}

.center-container {
  text-align: center;
  max-width: 680px;
  padding: 0 20px;
}

/* Hero 区域 - 增加留白 */
.hero {
  margin-bottom: 0;
  padding: 40px 0;
}

.hero-title {
  font-size: 56px;
  font-weight: 500;
  line-height: 1.25;
  color: #1a1a1a;
  margin-bottom: 32px;
  letter-spacing: -0.02em;
  font-family: 'Noto Serif SC', serif;
}

.hero-title .accent {
  color: #d97706;
  font-weight: 500;
}

.hero-description {
  font-size: 18px;
  line-height: 1.9;
  color: #666;
  margin-bottom: 48px;
  font-weight: 400;
  letter-spacing: 0.01em;
}

.hero-actions {
  margin-bottom: 32px;
}

.hero-links {
  margin-top: 28px;
}

.link {
  font-size: 15px;
  color: #888;
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-weight: 400;
}

.link:hover {
  color: #1a1a1a;
  text-decoration: underline;
  transform: translateY(-1px);
}

/* 功能特性区域 - 增加留白 */
.features-section {
  background: var(--glass-bg-heavy);
  backdrop-filter: blur(var(--blur-lg)) saturate(1.2);
  -webkit-backdrop-filter: blur(var(--blur-lg)) saturate(1.2);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-glass-md);
  padding: 140px 40px;
  position: relative;
  z-index: 10;
}

.features-container {
  max-width: 900px;
  margin: 0 auto;
}

.features {
  display: flex;
  justify-content: center;
  gap: 120px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.feature-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1a1a1a;
  background: #faf9f7;
  border-radius: 16px;
  transition: transform 0.3s ease;
}

.feature-icon:hover {
  transform: translateY(-4px);
}

.feature-icon svg {
  width: 28px;
  height: 28px;
}

.feature-item span {
  font-size: 16px;
  color: #555;
  font-weight: 500;
  letter-spacing: 0.02em;
}

/* 页脚 - 增加留白 */
.footer {
  position: relative;
  z-index: 10;
  padding: 60px 24px;
  text-align: center;
  background: #faf9f7;
  border-top: 1px solid #e8e8e8;
}

.footer p {
  font-size: 14px;
  color: #999;
  font-weight: 400;
  letter-spacing: 0.02em;
}

/* 背景装饰 - 边缘淡化效果 */
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
@media (max-width: 768px) {
  .navbar {
    padding: 20px 24px;
  }

  .main-content {
    padding: 80px 24px 40px;
  }

  .hero-title {
    font-size: 36px;
    margin-bottom: 24px;
  }

  .hero-description {
    font-size: 16px;
    margin-bottom: 36px;
  }

  .features-section {
    padding: 80px 24px;
  }

  .features {
    flex-direction: column;
    gap: 48px;
  }

  .btn-large {
    padding: 16px 40px;
    font-size: 15px;
  }
}
</style>
