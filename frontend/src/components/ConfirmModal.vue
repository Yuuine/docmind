<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="localVisible" class="confirm-modal-overlay" @click.self="handleClose">
        <div class="confirm-modal-box">
          <div class="confirm-modal-icon" v-if="isDestructive">
            <Icon name="warning" :size="32" />
          </div>
          <h3 class="confirm-modal-title">{{ title }}</h3>
          <p class="confirm-modal-message">{{ message }}</p>
          <div class="confirm-modal-actions">
            <button class="confirm-modal-btn confirm-modal-cancel" @click="handleCancel">
              {{ cancelText }}
            </button>
            <button class="confirm-modal-btn confirm-modal-confirm" :class="{ 'is-destructive': isDestructive }" @click="handleConfirm">
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { Icon } from '@/components/icons'

const props = defineProps<{
  modelValue: boolean
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  isDestructive?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const localVisible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (newVal) => {
    localVisible.value = newVal
  }
)

watch(localVisible, (newVal) => {
  emit('update:modelValue', newVal)
})

function handleClose() {
  localVisible.value = false
  emit('cancel')
}

function handleCancel() {
  localVisible.value = false
  emit('cancel')
}

function handleConfirm() {
  localVisible.value = false
  emit('confirm')
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && localVisible.value) {
    handleClose()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.confirm-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.confirm-modal-box {
  background: var(--glass-bg-heavy);
  backdrop-filter: blur(var(--blur-lg)) saturate(1.25);
  -webkit-backdrop-filter: blur(var(--blur-lg)) saturate(1.25);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-glass-lg);
  border-radius: var(--radius-lg);
  padding: 28px 32px;
  max-width: 420px;
  width: 90%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.confirm-modal-icon {
  width: 56px;
  height: 56px;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: #dc2626;
}

.confirm-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
  text-align: center;
}

.confirm-modal-message {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 24px;
  text-align: center;
}

.confirm-modal-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  width: 100%;
}

.confirm-modal-btn {
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.confirm-modal-cancel {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.confirm-modal-cancel:hover {
  background: var(--bg-secondary);
}

.confirm-modal-confirm {
  background: var(--color-accent);
  color: white;
}

.confirm-modal-confirm:hover {
  background: var(--color-accent-hover);
}

.confirm-modal-confirm.is-destructive {
  background: #dc2626;
}

.confirm-modal-confirm.is-destructive:hover {
  background: #b91c1c;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;
}

.modal-enter-active .confirm-modal-box,
.modal-leave-active .confirm-modal-box {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .confirm-modal-box,
.modal-leave-to .confirm-modal-box {
  transform: scale(0.95) translateY(-10px);
  opacity: 0;
}
</style>
