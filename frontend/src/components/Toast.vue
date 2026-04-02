<template>
  <div class="toast-container">
    <transition-group name="toast">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="toast"
        :class="toast.type"
        @click="toastStore.remove(toast.id)"
      >
        <span class="toast-message">{{ toast.message }}</span>
        <button class="toast-close" @click.stop="toastStore.remove(toast.id)">
          <Icon name="close" :size="14" />
        </button>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { useToastStore } from '@/stores/toast'
import { Icon } from '@/components/icons'

const toastStore = useToastStore()
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  min-width: 200px;
  max-width: 320px;
  pointer-events: auto;
  cursor: pointer;
  transition: all 0.3s ease;
}

.toast:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.toast-message {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}

.toast-close {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

/* 成功 - 绿色主题 */
.toast.success {
  background: #f0fdf4;
  border: 1px solid #86efac;
  box-shadow: 0 3px 12px rgba(34, 197, 94, 0.12);
}

.toast.success .toast-message {
  color: #166534;
}

.toast.success .toast-close {
  color: #22c55e;
}

.toast.success .toast-close:hover {
  background: #dcfce7;
}

/* 错误 - 红色主题 */
.toast.error {
  background: #fef2f2;
  border: 1px solid #fca5a5;
  box-shadow: 0 3px 12px rgba(220, 38, 38, 0.12);
}

.toast.error .toast-message {
  color: #991b1b;
}

.toast.error .toast-close {
  color: #dc2626;
}

.toast.error .toast-close:hover {
  background: #fee2e2;
}

/* 警告 - 橙色主题 */
.toast.warning {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  box-shadow: 0 3px 12px rgba(245, 158, 11, 0.12);
}

.toast.warning .toast-message {
  color: #92400e;
}

.toast.warning .toast-close {
  color: #f59e0b;
}

.toast.warning .toast-close:hover {
  background: #fef3c7;
}

/* 信息 - 蓝色主题 */
.toast.info {
  background: #eff6ff;
  border: 1px solid #93c5fd;
  box-shadow: 0 3px 12px rgba(59, 130, 246, 0.12);
}

.toast.info .toast-message {
  color: #1e40af;
}

.toast.info .toast-close {
  color: #3b82f6;
}

.toast.info .toast-close:hover {
  background: #dbeafe;
}

/* 进入/离开动画 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

/* 响应式 */
@media (max-width: 480px) {
  .toast-container {
    top: 12px;
    right: 12px;
    left: 12px;
  }

  .toast {
    min-width: auto;
    max-width: none;
  }
}
</style>
