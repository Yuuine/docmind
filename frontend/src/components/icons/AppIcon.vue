<template>
  <svg
    :viewBox="viewBox"
    :width="size"
    :height="size"
    :fill="fill"
    :stroke="stroke"
    :stroke-width="strokeWidth"
    xmlns="http://www.w3.org/2000/svg"
  >
    <slot />
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  viewBox?: string
  size?: number | string
  fill?: string
  stroke?: string
  strokeWidth?: number | string
}>(), {
  viewBox: '0 0 24 24',
  size: 24,
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2
})
</script>
