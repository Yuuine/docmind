import { prepare, layout } from '@chenglou/pretext'

export function measureTextHeight(
  text: string,
  font: string,
  maxWidth: number,
  lineHeight: number
): number {
  const prepared = prepare(text, font)
  const { height } = layout(prepared, maxWidth, lineHeight)
  return height
}

export function measureTextareaHeight(
  text: string,
  font: string,
  width: number,
  lineHeight: number
): number {
  const prepared = prepare(text, font, {
    whiteSpace: 'pre-wrap',
  })
  const { height } = layout(prepared, width, lineHeight)
  return height
}
