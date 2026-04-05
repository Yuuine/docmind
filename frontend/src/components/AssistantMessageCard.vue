<template>
  <div class="assistant-message-card">
    <div class="assistant-message-card__surface">
      <slot />
    </div>
    <div v-if="$slots.footer" class="assistant-message-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<style scoped>
.assistant-message-card {
  width: 100%;
  text-align: left;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.assistant-message-card__surface {
  width: 100%;
  max-width: var(--assistant-card-max-width);
  padding: var(--assistant-card-padding);
  background: var(--assistant-card-bg);
  border-radius: var(--assistant-card-radius);
  box-shadow: var(--assistant-card-shadow);
}

.assistant-message-card__footer {
  width: 100%;
  max-width: var(--assistant-card-max-width);
  padding-top: 2px;
}

.assistant-message-card__footer :deep(.message-meta-row) {
  margin-top: 0;
}

.assistant-message-card__footer :deep(.message-time) {
  margin-top: 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-tertiary);
  letter-spacing: 0.02em;
  opacity: 0.92;
}
</style>
