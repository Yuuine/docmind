<template>
  <AppIcon
    :viewBox="viewBox"
    :size="size"
    :fill="fill"
    :stroke="stroke"
    :strokeWidth="strokeWidth"
    v-html="iconPath"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { IconPaths, type IconName } from './Icons'

const props = withDefaults(defineProps<{
  name: IconName
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

const iconPath = computed(() => IconPaths[props.name] || '')
</script>
