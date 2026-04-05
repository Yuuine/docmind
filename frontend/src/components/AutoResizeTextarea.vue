<template>
  <div class="auto-resize-textarea" :class="{ focused: isFocused, disabled: disabled }">
    <div v-if="$slots.prefix" class="textarea-prefix">
      <slot name="prefix" />
    </div>

    <textarea
      ref="textareaRef"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :rows="minRows"
      class="textarea-inner"
      :class="{ 'has-overflow': isOverflowing }"
      @input="onInput"
      @keydown="onKeydown"
      @focus="onFocus"
      @blur="onBlur"
    />

    <div v-if="$slots.suffix" class="textarea-suffix">
      <slot name="suffix" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import { measureTextareaHeight } from '@/utils/textMeasure'

interface Props {
  modelValue: string
  placeholder?: string
  disabled?: boolean
  minRows?: number
  maxRows?: number
  maxHeight?: number | string
  enterBehavior?: 'send' | 'newline'
  autoResize?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '',
  disabled: false,
  minRows: 1,
  maxRows: 8,
  enterBehavior: 'send',
  autoResize: true,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'send', value: string): void
  (e: 'focus', event: FocusEvent): void
  (e: 'blur', event: FocusEvent): void
  (e: 'input', event: Event): void
}>()

const textareaRef = ref<HTMLTextAreaElement>()
const isFocused = ref(false)
const isOverflowing = ref(false)

function getLineHeight(): number {
  if (!textareaRef.value) return 22
  const style = getComputedStyle(textareaRef.value)
  return parseFloat(style.lineHeight) || 22
}

function getFontString(): string {
  if (!textareaRef.value) return '15px Inter, sans-serif'
  const style = getComputedStyle(textareaRef.value)
  return `${style.fontSize} ${style.fontFamily}`
}

function resolveMaxHeight(): number {
  if (props.maxHeight !== undefined) {
    return typeof props.maxHeight === 'number' ? props.maxHeight : parseInt(String(props.maxHeight)) || 0
  }
  if (props.maxRows && props.maxRows > 0) {
    return props.maxRows * getLineHeight()
  }
  return 0
}

function updateHeight() {
  if (!props.autoResize || !textareaRef.value) return
  nextTick(() => {
    const el = textareaRef.value!
    const font = getFontString()
    const width = el.clientWidth - 28
    const lineHeight = getLineHeight()

    const measuredHeight = measureTextareaHeight(props.modelValue || ' ', font, width, lineHeight)
    const minHeight = props.minRows * lineHeight
    let targetHeight = Math.max(measuredHeight, minHeight)

    const maxH = resolveMaxHeight()
    if (maxH > 0 && targetHeight > maxH) {
      targetHeight = maxH
      isOverflowing.value = true
    } else {
      isOverflowing.value = false
    }

    el.style.height = `${targetHeight}px`
  })
}

function onInput(e: Event) {
  const target = e.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
  emit('input', e)
  updateHeight()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    if (props.enterBehavior === 'send') {
      e.preventDefault()
      emit('send', props.modelValue)
    }
  }
}

function onFocus(e: FocusEvent) {
  isFocused.value = true
  emit('focus', e)
}

function onBlur(e: FocusEvent) {
  isFocused.value = false
  emit('blur', e)
}

watch(() => props.modelValue, () => updateHeight())
onMounted(() => updateHeight())

defineExpose({
  focus: () => textareaRef.value?.focus(),
  blur: () => textareaRef.value?.blur(),
  textareaRef,
})
</script>

<style scoped>
.auto-resize-textarea {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 14px;
  border: 1.5px solid #e5e5e5;
  border-radius: 12px;
  background: #faf9f7;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

.auto-resize-textarea.focused {
  border-color: var(--color-accent);
  background: white;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-accent) 18%, transparent);
}

.auto-resize-textarea.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f5f5f5;
}

.textarea-inner {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  line-height: 1.5;
  font-family: inherit;
  color: #333;
  background: transparent;
  overflow-y: hidden;
  min-height: 1.5em;
}

.textarea-inner.has-overflow {
  overflow-y: auto;
}

.textarea-inner::placeholder {
  color: #aaa;
}

.textarea-prefix,
.textarea-suffix {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .auto-resize-textarea {
    padding: 12px 16px;
    border-radius: 14px;
  }

  .textarea-inner {
    font-size: 16px;
  }
}

.auto-resize-textarea:focus-within:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}
</style>
