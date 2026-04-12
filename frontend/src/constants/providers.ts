import type { ModelProviderType } from '@/types'

const PROVIDER_LABELS: Record<string, string> = {
  DEEPSEEK: 'DeepSeek',
  OPENAI: 'OpenAI',
  MOONSHOT: 'Kimi',
  QWEN: '千问'
}

export function getProviderLabel(providerType?: ModelProviderType | string): string {
  if (providerType == null || providerType === '') return ''
  return PROVIDER_LABELS[providerType] ?? providerType
}
