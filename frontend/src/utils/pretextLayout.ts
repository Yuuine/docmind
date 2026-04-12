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
/** 短文本阈值：低于此值使用快速路径 */
const SHORT_TEXT_THRESHOLD = 256
/** Markdown结果缓存上限 */
const MARKDOWN_RESULT_CACHE_LIMIT = 48

const prepareCache = new Map<string, PreparedText>()
const markdownResultCache = new Map<string, { height: number; lineCount: number }>()

let cacheHits = 0
let cacheMisses = 0

function touchPrepareCache(key: string, factory: () => PreparedText): PreparedText {
  const existing = prepareCache.get(key)
  if (existing) {
    cacheHits++
    prepareCache.delete(key)
    prepareCache.set(key, existing)
    return existing
  }
  cacheMisses++
  const prepared = factory()
  evictPrepareCacheIfNeeded()
  prepareCache.set(key, prepared)
  return prepared
}

function evictPrepareCacheIfNeeded(): void {
  while (prepareCache.size >= PREPARE_CACHE_LIMIT) {
    const first = prepareCache.keys().next().value
    if (first === undefined) break
    prepareCache.delete(first)
  }
}

function prepareCacheKey(text: string, font: string, whiteSpace: 'normal' | 'pre-wrap'): string {
  return `${whiteSpace}\0${font}\0${text}`
}

function markdownResultCacheKey(markdown: string, maxWidthPx: number, font: string, lineHeightPx: number): string {
  return `${maxWidthPx}\0${font}\0${lineHeightPx}\0${markdown}`
}

/**
 * 将 Markdown 压成近似纯文本，用于流式阶段估算高度（不触发 DOM 测量）。
 */
export function stripMarkdownForLayoutEstimate(source: string): string {
  if (!source) return ''
  let s = source
  
  // 处理代码块：保留换行但移除内容
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
  
  // 行内代码：用占位符替换
  s = s.replace(/`[^`\n]+`/g, (m) => '·'.repeat(Math.min(Math.max(m.length - 2, 1), 32)))
  
  // 移除标题标记但保留额外行高空间
  s = s.replace(/^#{1,6}\s+/gm, '')
  
  // 引用块
  s = s.replace(/^\s*>\s?/gm, '')
  
  // 链接：只保留文本
  s = s.replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
  
  // 粗体、斜体、删除线标记
  s = s.replace(/(\*\*|__|~~)/g, '')
  
  // 表格：保留换行趋势
  s = s.replace(/^\s*\|.*\|\s*$/gm, (line) => line.replace(/\|/g, ' '))
  
  // 图片：用占位符替换
  s = s.replace(/!\[([^\]]*)\]\([^)]*\)/g, '[图片]')
  
  // 水平线：转换为空行
  s = s.replace(/^---+$/gm, '')
  
  return s.trim() ? s : '\n'
}

function fallbackHeight(text: string, maxWidthPx: number, lineHeightPx: number): number {
  const approxChars = Math.max(24, Math.floor(maxWidthPx / 9))
  const wrappedLines = Math.max(1, Math.ceil(text.length / approxChars))
  const hardLines = text.split('\n').length
  const lines = Math.max(wrappedLines, hardLines)
  return lines * lineHeightPx
}

function fallbackLineCount(text: string, maxWidthPx: number): number {
  const approxChars = Math.max(24, Math.floor(maxWidthPx / 9))
  const wrappedLines = Math.max(1, Math.ceil(text.length / approxChars))
  const hardLines = text.split('\n').length
  return Math.max(wrappedLines, hardLines)
}

/**
 * 计算Markdown元素的额外高度（标题、代码块等）
 */
function calculateMarkdownExtraHeight(markdown: string, lineHeightPx: number): number {
  let extraHeight = 0
  
  // 标题额外高度（h1-h6）
  const headings = markdown.match(/^#{1,6}\s+/gm)
  if (headings) {
    extraHeight += headings.length * lineHeightPx * 0.5
  }
  
  // 代码块额外高度（每块增加一些padding）
  const codeBlocks = (markdown.match(/```[\s\S]*?```/g) || []).length
  extraHeight += codeBlocks * lineHeightPx * 1.5
  
  // 引用块额外高度
  const blockquotes = (markdown.match(/^\s*>/gm) || []).length
  extraHeight += Math.min(blockquotes, 10) * lineHeightPx * 0.3
  
  // 列表项额外缩进
  const listItems = (markdown.match(/^\s*[-*+]\s|^\d+\.\s/gm) || []).length
  extraHeight += listItems * lineHeightPx * 0.2
  
  return extraHeight
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
  
  // 超长文本降级
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
 * 使用 Pretext 进行纯算术计算，不触发 DOM 重排。
 * 
 * 性能优化：
 * - 结果缓存层（避免重复 strip + 计算）
 * - 短文本快速路径
 * - 超长文本自动降级
 */
export function measureMarkdownRoughHeight(
  markdown: string,
  maxWidthPx: number,
  font: string = CHAT_MARKDOWN_FONT,
  lineHeightPx: number = CHAT_LINE_HEIGHT_PX
): number {
  if (!markdown?.trim()) return 0
  
  // 检查结果缓存
  const resultKey = markdownResultCacheKey(markdown, maxWidthPx, font, lineHeightPx)
  const cached = markdownResultCache.get(resultKey)
  if (cached) {
    return cached.height
  }
  
  // 超长文本降级（在strip之前检查原始长度）
  if (markdown.length > MAX_CHARS_FOR_PREPARE) {
    const plain = stripMarkdownForLayoutEstimate(markdown)
    const baseHeight = fallbackHeight(plain, maxWidthPx, lineHeightPx)
    const extraHeight = calculateMarkdownExtraHeight(markdown, lineHeightPx)
    const total = baseHeight + extraHeight
    
    // 不缓存超长文本结果（避免内存浪费）
    return total
  }
  
  const plain = stripMarkdownForLayoutEstimate(markdown)
  const baseHeight = measurePlainBlockHeight(plain, maxWidthPx, font, lineHeightPx)
  const extraHeight = calculateMarkdownExtraHeight(markdown, lineHeightPx)
  const total = baseHeight + extraHeight
  
  // 缓存结果（仅缓存合理大小的文本）
  if (markdown.length <= MAX_CHARS_FOR_PREPARE) {
    cacheMarkdownResult(resultKey, { height: total, lineCount: Math.ceil(total / lineHeightPx) })
  }
  
  return total
}

/**
 * 快速估算文本行数，用于虚拟列表初始渲染。
 * 
 * 特性：
 * - 使用 Pretext 的精确布局引擎
 * - LRU 缓存复用已准备的文本
 * - 超长文本自动降级为字符数估算
 * - 支持自定义 whiteSpace 模式
 * 
 * @param text 要测量的文本
 * @param maxWidthPx 容器最大宽度（像素）
 * @param font 字体描述（默认为聊天字体）
 * @param options.whiteSpace 空白处理模式
 * @returns 估算的行数（至少为1）
 */
export function estimateLineCount(
  text: string,
  maxWidthPx: number,
  font: string = CHAT_MARKDOWN_FONT,
  options?: { whiteSpace?: 'normal' | 'pre-wrap' }
): number {
  if (!text?.trim()) return 1
  
  const t = text.trim()
  const w = Math.max(120, maxWidthPx)
  const whiteSpace = options?.whiteSpace ?? 'normal'
  
  // 超长文本降级：使用字符数估算
  if (t.length > MAX_CHARS_FOR_PREPARE) {
    return fallbackLineCount(t, w)
  }
  
  try {
    const key = prepareCacheKey(t, font, whiteSpace)
    const prepared = touchPrepareCache(key, () => prepare(t, font, { whiteSpace }))
    const result = layout(prepared, w, CHAT_LINE_HEIGHT_PX)
    return result.lineCount
  } catch {
    // 异常降级
    return fallbackLineCount(t, w)
  }
}

/**
 * 手动清理所有 Pretext 布局缓存。
 * 
 * @returns 清理统计信息
 * @example
 * // 在内存紧张时调用
 * const stats = clearPrepareLayoutCache()
 * console.log(`清理了 ${stats.cleared} 条缓存`)
 */
export function clearPrepareLayoutCache(): { 
  cleared: number; 
  prepareCacheSize: number; 
  resultCacheSize: number;
  hitRate: number;
} {
  const prepareSize = prepareCache.size
  const resultSize = markdownResultCache.size
  const totalCleared = prepareSize + resultSize
  
  prepareCache.clear()
  markdownResultCache.clear()
  
  // 重置统计
  const previousHits = cacheHits
  const previousMisses = cacheMisses
  cacheHits = 0
  cacheMisses = 0
  
  return {
    cleared: totalCleared,
    prepareCacheSize: prepareSize,
    resultCacheSize: resultSize,
    hitRate: previousHits + previousMisses > 0 ? previousHits / (previousHits + previousMisses) : 0
  }
}

/**
 * 获取当前缓存统计信息（不清理）。
 */
export function getPrepareLayoutCacheStats(): {
  prepareCacheSize: number;
  resultCacheSize: number;
  hits: number;
  misses: number;
  hitRate: number;
} {
  const total = cacheHits + cacheMisses
  return {
    prepareCacheSize: prepareCache.size,
    resultCacheSize: markdownResultCache.size,
    hits: cacheHits,
    misses: cacheMisses,
    hitRate: total > 0 ? cacheHits / total : 0
  }
}

function cacheMarkdownResult(key: string, value: { height: number; lineCount: number }): void {
  // LRU淘汰
  while (markdownResultCache.size >= MARKDOWN_RESULT_CACHE_LIMIT) {
    const first = markdownResultCache.keys().next().value
    if (first === undefined) break
    markdownResultCache.delete(first)
  }
  markdownResultCache.set(key, value)
}
