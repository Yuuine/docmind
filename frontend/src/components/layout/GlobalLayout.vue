<template>
  <div class="global-layout">
    <nav class="navbar">
      <div class="nav-left">
        <div class="nav-brand">
          <Icon name="logo" :size="24" />
          <span class="brand-text">DocMind</span>
        </div>
      </div>

      <div class="nav-center">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: route.path === item.path }"
        >
          <Icon :name="item.icon" :size="16" />
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </div>

      <div class="nav-right">
        <div class="user-avatar-wrapper" ref="avatarRef" @click.stop="toggleUserMenu">
          <div class="user-avatar">
            {{ userAvatarLetter }}
          </div>
          <transition name="dropdown-fade">
            <div v-if="showUserMenu" class="user-dropdown">
              <div class="dropdown-user-info">
                <span class="dropdown-username">{{ userStore.user?.username }}</span>
              </div>
              <div class="menu-divider"></div>
              <button class="dropdown-item" @click="openSettings">
                <Icon name="settings" :size="16" />
                <span>设置</span>
              </button>
              <button class="dropdown-item dropdown-item-danger" @click.stop="showLogoutConfirm = true">
                <Icon name="logout" :size="16" />
                <span>退出登录</span>
              </button>
            </div>
          </transition>
        </div>
      </div>
    </nav>

    <main class="main-content">
      <router-view v-slot="{ Component, route }">
        <transition name="page-transition" mode="out-in" appear>
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>

    <SettingsModal v-model="showSettingsModal" />

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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Icon } from '@/components/icons'
import SettingsModal from '@/components/SettingsModal.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const avatarRef = ref<HTMLElement>()
const showUserMenu = ref(false)
const showSettingsModal = ref(false)
const showLogoutConfirm = ref(false)

const userAvatarLetter = computed(() => {
  const name = userStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function openSettings() {
  showUserMenu.value = false
  showSettingsModal.value = true
}

function handleClickOutside(event: MouseEvent) {
  if (avatarRef.value && !avatarRef.value.contains(event.target as Node)) {
    showUserMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

const navItems = [
  { path: '/documents', label: '文档', icon: 'document' as const },
  { path: '/chat', label: '对话', icon: 'message' as const }
]

function handleLogout() {
  userStore.logout()
  router.push('/')
}
</script>

<style scoped>
.global-layout {
  min-height: 100vh;
  background: #faf9f7;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 56px;
  padding: 0 40px;
  background: var(--glass-bg-heavy);
  backdrop-filter: blur(var(--blur-lg)) saturate(1.2);
  -webkit-backdrop-filter: blur(var(--blur-lg)) saturate(1.2);
  box-shadow: 0 1px 0 rgba(0,0,0,0.03), var(--shadow-glass-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-left {
  display: flex;
  align-items: center;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #1a1a1a;
}

.brand-text {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.nav-center {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s ease;
}

.nav-link:hover {
  color: #1a1a1a;
  background: #f5f5f5;
}

.nav-link.active,
.nav-link.router-link-exact-active {
  color: #1a1a1a;
  font-weight: 500;
  background: rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(4px);
}

.nav-right {
  display: flex;
  align-items: center;
  position: relative;
}

.user-avatar-wrapper {
  position: relative;
  cursor: pointer;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--glass-bg-heavy);
  backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  -webkit-backdrop-filter: blur(var(--blur-md)) saturate(1.15);
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-glass-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  user-select: none;
}

.user-avatar:hover {
  transform: scale(1.08);
  box-shadow: var(--shadow-glass-md);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  min-width: 160px;
  background: #ffffff;
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  padding: 8px 0;
  z-index: 200;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.dropdown-user-info {
  padding: 12px 16px 8px;
}

.dropdown-username {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.menu-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 4px 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  text-align: left;
  font-family: inherit;
  transition: background 0.15s ease, color 0.15s ease;
}

.dropdown-item:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #1a1a1a;
}

.dropdown-item-danger {
  color: #ef4444;
}

.dropdown-item-danger:hover {
  background: #fef2f2;
  color: #dc2626;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.96);
}

.main-content {
  padding-top: 56px;
  min-height: calc(100vh - 56px);
}

/* ========== Page Transition ========== */
.page-transition-enter-active {
  transition: opacity 0.25s ease-out, transform 0.25s ease-out;
}

.page-transition-leave-active {
  transition: opacity 0.18s ease-in, transform 0.18s ease-in;
}

.page-transition-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-transition-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (prefers-reduced-motion: reduce) {
  .page-transition-enter-active,
  .page-transition-leave-active {
    transition: none;
  }

  .page-transition-enter-from,
  .page-transition-leave-to {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
  }

  .brand-text {
    display: none;
  }

  .nav-label {
    display: none;
  }

  .nav-link {
    padding: 8px 12px;
  }
}
</style>
