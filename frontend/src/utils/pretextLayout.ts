/**
 * 与聊天正文 / 设计 token 对齐的 Pretext 封装。
 * @see https://github.com/chenglou/pretext
 */
import { prepare, layout, type PreparedText } from '@chenglou/pretext'

/** 与 global.css / Markdown 正文一致：16px、行高约 1.7 */
export const CHAT_MARKDOWN_FONT = '400 16px Inter, "Noto Sans SC", sans-serif'
export const CHAT_LINE_HEIGHT_PX = 27.2

const PREPARE_CACHE_LIMIT = 96
/** 超过此长度不再调用 prepare，避免流式长文阻塞主线程 */
const MAX_CHARS_FOR_PREPARE = 72_000
const prepareCache = new Map<string, PreparedText>()

function touchPrepareCache(key: string, factory: () => PreparedText): PreparedText {
  const existing = prepareCache.get(key)
  if (existing) {
    prepareCache.delete(key)
    prepareCache.set(key, existing)
    return existing
  }
  const prepared = factory()
  while (prepareCache.size >= PREPARE_CACHE_LIMIT) {
    const first = prepareCache.keys().next().value
    if (first === undefined) break
    prepareCache.delete(first)
  }
  prepareCache.set(key, prepared)
  return prepared
}

function prepareCacheKey(text: string, font: string, whiteSpace: 'normal' | 'pre-wrap'): string {
  return `${whiteSpace}\0${font}\0${text}`
}

/**
 * 将 Markdown 压成近似纯文本，用于流式阶段估算高度（不触发 DOM 测量）。
 */
export function stripMarkdownForLayoutEstimate(source: string): string {
  if (!source) return ''
  let s = source
  s = s.replace(/```[\w.-]*\n?([\s\S]*?)```/gm, (_m, code: string) => {
    const lines = code.split('\n').length + 2
    return '\n'.repeat(Math.max(lines, 1))
  })
  // 流式输出末尾可能只有未闭合的 ```，按代码块占位行数估算
  const tickMatches = s.match(/```/g)
  if (tickMatches && tickMatches.length % 2 === 1) {
    const idx = s.lastIndexOf('```')
    const tail = s.slice(idx)
    const codeOnly = tail.replace(/^```[\w.-]*\n?/, '')
    const lines = codeOnly.split('\n').length + 2
    s = s.slice(0, idx) + '\n'.repeat(Math.max(lines, 1))
  }
  s = s.replace(/`[^`\n]+`/g, (m) => '·'.repeat(Math.min(Math.max(m.length - 2, 1), 32)))
  s = s.replace(/^#{1,6}\s+/gm, '')
  s = s.replace(/^\s*>\s?/gm, '')
  s = s.replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
  s = s.replace(/(\*\*|__|~~)/g, '')
  // 表格：保留换行趋势
  s = s.replace(/^\s*\|.*\|\s*$/gm, (line) => line.replace(/\|/g, ' '))
  return s.trim() ? s : '\n'
}

function fallbackHeight(text: string, maxWidthPx: number, lineHeightPx: number): number {
  const approxChars = Math.max(24, Math.floor(maxWidthPx / 9))
  const wrappedLines = Math.max(1, Math.ceil(text.length / approxChars))
  const hardLines = text.split('\n').length
  const lines = Math.max(wrappedLines, hardLines)
  return lines * lineHeightPx
}

/**
 * 纯文本块高度（与用户气泡、估算用同一套逻辑）。
 */
export function measurePlainBlockHeight(
  text: string,
  maxWidthPx: number,
  font: string = CHAT_MARKDOWN_FONT,
  lineHeightPx: number = CHAT_LINE_HEIGHT_PX,
  options?: { whiteSpace?: 'normal' | 'pre-wrap' }
): number {
  const t = text.trim()
  if (!t) return 0
  const w = Math.max(120, maxWidthPx)
  const whiteSpace = options?.whiteSpace ?? 'normal'
  if (t.length > MAX_CHARS_FOR_PREPARE) {
    return fallbackHeight(t, w, lineHeightPx)
  }
  try {
    const key = prepareCacheKey(t, font, whiteSpace)
    const prepared = touchPrepareCache(key, () => prepare(t, font, { whiteSpace }))
    return layout(prepared, w, lineHeightPx).height
  } catch {
    return fallbackHeight(t, w, lineHeightPx)
  }
}

/**
 * 流式 Markdown 的粗略高度（先 strip 再测量，避免代码块/语法符号干扰换行估算）。
 */
export function measureMarkdownRoughHeight(
  markdown: string,
  maxWidthPx: number,
  font: string = CHAT_MARKDOWN_FONT,
  lineHeightPx: number = CHAT_LINE_HEIGHT_PX
): number {
  const plain = stripMarkdownForLayoutEstimate(markdown)
  return measurePlainBlockHeight(plain, maxWidthPx, font, lineHeightPx)
}

export function clearPrepareLayoutCache(): void {
  prepareCache.clear()
}
