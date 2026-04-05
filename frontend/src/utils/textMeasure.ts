import { measurePlainBlockHeight } from '@/utils/pretextLayout'

export { CHAT_MARKDOWN_FONT, CHAT_LINE_HEIGHT_PX } from '@/utils/pretextLayout'

export function measureTextHeight(
  text: string,
  font: string,
  maxWidth: number,
  lineHeight: number
): number {
  return measurePlainBlockHeight(text, maxWidth, font, lineHeight)
}

export function measureTextareaHeight(
  text: string,
  font: string,
  width: number,
  lineHeight: number
): number {
  return measurePlainBlockHeight(text, width, font, lineHeight, { whiteSpace: 'pre-wrap' })
}
