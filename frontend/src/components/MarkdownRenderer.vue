<template>
  <div ref="rootRef" class="markdown-renderer" :style="rendererMinHeightStyle">
    <template v-if="isStreamingEmpty">
      <div class="streaming-ellipsis" aria-live="polite">
        <span class="streaming-ellipsis__dot"></span>
        <span class="streaming-ellipsis__dot"></span>
        <span class="streaming-ellipsis__dot"></span>
      </div>
    </template>
    <template v-else>
      <div ref="contentRef" v-html="renderedContent" class="markdown-content"></div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useElementWidth } from '@/composables/useElementWidth'
import { measureMarkdownRoughHeight, CHAT_LINE_HEIGHT_PX } from '@/utils/pretextLayout'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import cpp from 'highlight.js/lib/languages/cpp'
import c from 'highlight.js/lib/languages/c'
import csharp from 'highlight.js/lib/languages/csharp'
import html from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import scss from 'highlight.js/lib/languages/scss'
import sql from 'highlight.js/lib/languages/sql'
import json from 'highlight.js/lib/languages/json'
import yaml from 'highlight.js/lib/languages/yaml'
import bash from 'highlight.js/lib/languages/bash'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import php from 'highlight.js/lib/languages/php'
import ruby from 'highlight.js/lib/languages/ruby'
import kotlin from 'highlight.js/lib/languages/kotlin'
import swift from 'highlight.js/lib/languages/swift'
import dart from 'highlight.js/lib/languages/dart'
import markdown from 'highlight.js/lib/languages/markdown'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('jsx', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('tsx', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('python3', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('c++', cpp)
hljs.registerLanguage('c', c)
hljs.registerLanguage('csharp', csharp)
hljs.registerLanguage('cs', csharp)
hljs.registerLanguage('html', html)
hljs.registerLanguage('htm', html)
hljs.registerLanguage('xml', html)
hljs.registerLanguage('xhtml', html)
hljs.registerLanguage('css', css)
hljs.registerLanguage('scss', scss)
hljs.registerLanguage('sass', scss)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('mysql', sql)
hljs.registerLanguage('postgresql', sql)
hljs.registerLanguage('sqlite', sql)
hljs.registerLanguage('json', json)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('yml', yaml)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('go', go)
hljs.registerLanguage('golang', go)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('rs', rust)
hljs.registerLanguage('php', php)
hljs.registerLanguage('ruby', ruby)
hljs.registerLanguage('rb', ruby)
hljs.registerLanguage('kotlin', kotlin)
hljs.registerLanguage('kt', kotlin)
hljs.registerLanguage('swift', swift)
hljs.registerLanguage('dart', dart)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)

interface Props {
  content: string
  isStreaming?: boolean
}

const props = defineProps<Props>()

const rootRef = ref<HTMLElement | null>(null)
const contentRef = ref<HTMLElement | null>(null)
const contentWidthPx = useElementWidth(rootRef)
const renderedContent = ref('')
const streamingMinHeightPx = ref(0)
let roughHeightTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 使用 Pretext 快速估算 Markdown 高度
 * - 纯算术计算，不触发 DOM 重排/重绘
 * - 考虑标题、代码块、列表等额外高度
 * - 只增不减，避免高度回缩导致闪烁
 * - 配合 useStreamBuffer 的 rAF 节奏，无需额外延迟
 */
function scheduleStreamingRoughHeight() {
  if (roughHeightTimer) {
    clearTimeout(roughHeightTimer)
    roughHeightTimer = null
  }
  if (!props.isStreaming || !props.content.trim()) {
    streamingMinHeightPx.value = 0
    return
  }

  const cap = typeof window !== 'undefined' ? window.innerHeight * 0.92 : 1e6

  const h = measureMarkdownRoughHeight(props.content, contentWidthPx.value)
  const next = Math.min(Math.ceil(h), cap)

  streamingMinHeightPx.value = Math.max(streamingMinHeightPx.value, next)
}

const rendererMinHeightStyle = computed(() => {
  if (!props.isStreaming || streamingMinHeightPx.value <= 0) return undefined
  return { minHeight: `${streamingMinHeightPx.value}px` }
})

watch(
  () => [props.content, props.isStreaming, contentWidthPx.value] as const,
  () => scheduleStreamingRoughHeight(),
  { flush: 'post' }
)

watch(
  () => props.isStreaming,
  streaming => {
    if (!streaming) {
      if (roughHeightTimer) {
        clearTimeout(roughHeightTimer)
        roughHeightTimer = null
      }
      streamingMinHeightPx.value = 0
    }
  }
)

onUnmounted(() => {
  if (roughHeightTimer) clearTimeout(roughHeightTimer)
})

const isStreamingEmpty = computed(
  () => Boolean(props.isStreaming && !props.content.trim())
)

const fenceLanguageLabel = (lang: string | undefined): string => {
  const raw = (lang?.trim() || 'text').toLowerCase()
  return raw || 'text'
}

const copySvg = `<svg class="copy-button__icon" xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>`
const checkSvg = `<svg class="copy-button__icon" xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"></polyline></svg>`

const buildLineGutterHtml = (source: string): string => {
  const n = source.length === 0 ? 1 : source.split('\n').length
  return Array.from({ length: n }, (_, i) => `<span class="code-ln">${i + 1}</span>`).join('')
}

const handleCopy = async (code: string, button: HTMLButtonElement) => {
  try {
    await navigator.clipboard.writeText(code)
    button.innerHTML = checkSvg
    button.classList.add('copied')
    button.setAttribute('title', '已复制')

    setTimeout(() => {
      button.innerHTML = copySvg
      button.classList.remove('copied')
      button.setAttribute('title', '复制')
    }, 2000)
  } catch (err) {
    console.error('Copy failed:', err)
  }
}

const bindCopyEvents = () => {
  if (!contentRef.value) return

  const copyButtons = contentRef.value.querySelectorAll('.copy-button') as NodeListOf<HTMLButtonElement>
  copyButtons.forEach(button => {
    if (!button.dataset.bound) {
      button.dataset.bound = 'true'
      button.addEventListener('click', () => {
        const code = button.dataset.code || ''
        handleCopy(code, button)
      })
    }
  })
}

const renderer = new marked.Renderer()
renderer.code = function({ text, lang }: { text: string; lang?: string }) {
  const validLang = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
  let highlighted = text
  try {
    highlighted = hljs.highlight(text, { language: validLang }).value
  } catch (e) {
    try {
      highlighted = hljs.highlightAuto(text).value
    } catch (e2) {
      highlighted = DOMPurify.sanitize(text)
    }
  }

  const displayLang = fenceLanguageLabel(lang)
  const escapedCode = text.replace(/"/g, '&quot;').replace(/'/g, '&#39;')
  const gutterHtml = buildLineGutterHtml(text)

  return `
    <div class="code-block-wrapper">
      <div class="code-header">
        <span class="code-language">${displayLang}</span>
        <button type="button" class="copy-button" data-code="${escapedCode}" title="复制" aria-label="复制代码">
          ${copySvg}
        </button>
      </div>
      <div class="code-body">
        <div class="code-line-gutter">${gutterHtml}</div>
        <pre class="code-pre"><code class="hljs language-${validLang}">${highlighted}</code></pre>
      </div>
    </div>
  `
}

marked.setOptions({
  renderer: renderer,
  breaks: true,
  gfm: true
})

/**
 * 更新渲染内容
 * - 流式输出时：立即更新（useStreamBuffer 已通过 rAF 控制节奏）
 * - 完整内容时：立即更新，保证及时性
 */
function updateRenderedContent(content: string) {
  if (!content) {
    renderedContent.value = ''
    return
  }

  const html = marked.parse(content) as string
  renderedContent.value = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'p', 'br', 'hr',
      'strong', 'em', 'b', 'i', 'del', 'ins',
      'ul', 'ol', 'li',
      'a', 'blockquote', 'code', 'pre',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'div', 'span', 'button', 'svg', 'rect', 'path', 'polyline'
    ],
    ALLOWED_ATTR: [
      'href', 'target', 'rel', 'class', 'data-language', 'data-code',
      'xmlns', 'width', 'height', 'viewBox', 'fill', 'stroke', 'stroke-width',
      'stroke-linecap', 'stroke-linejoin', 'points', 'd', 'x', 'y', 'rx', 'ry',
      'style', 'type', 'title', 'aria-label', 'aria-hidden'
    ]
  })

  nextTick(() => bindCopyEvents())
}

watch(() => props.content, (newContent) => {
  updateRenderedContent(newContent)
}, { immediate: true })

onMounted(() => {
  updateRenderedContent(props.content)
})
</script>

<style scoped>
.markdown-renderer {
  text-align: left;
  width: 100%;
}

.streaming-ellipsis {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  min-height: 1.5em;
}

.streaming-ellipsis__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-secondary);
  animation: streaming-dot 1.2s ease-in-out infinite;
}

.streaming-ellipsis__dot:nth-child(2) {
  animation-delay: 0.15s;
}

.streaming-ellipsis__dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes streaming-dot {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.35;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}
</style>

<style>
/* v-html 内节点无 scoped 标记：Markdown 与代码块样式放在非 scoped 块，并用 .markdown-renderer 限制范围 */
.markdown-renderer .markdown-content {
  font-family: Inter, Roboto, Helvetica, PingFang SC, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-size: 16px;
  line-height: 1.7;
  color: var(--markdown-text);
  text-align: left;
  width: 100%;
}

.markdown-renderer .markdown-content :first-child {
  margin-top: 0;
}

.markdown-renderer .markdown-content :last-child {
  margin-bottom: 0;
}

.markdown-renderer .markdown-content h1,
.markdown-renderer .markdown-content h2,
.markdown-renderer .markdown-content h3,
.markdown-renderer .markdown-content h4,
.markdown-renderer .markdown-content h5,
.markdown-renderer .markdown-content h6 {
  margin-top: 1.5em;
  margin-bottom: 0.6em;
  line-height: 1.35;
  color: var(--markdown-text);
}

.markdown-renderer .markdown-content h1 {
  font-size: 1.75em;
  font-weight: 700;
  border-bottom: 2px solid var(--border-light);
  padding-bottom: 0.3em;
}

.markdown-renderer .markdown-content h2 {
  font-size: 1.45em;
  font-weight: 600;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 0.25em;
}

.markdown-renderer .markdown-content h3 {
  font-size: 1.25em;
  font-weight: 600;
}

.markdown-renderer .markdown-content h4 {
  font-size: 1.1em;
  font-weight: 600;
}

.markdown-renderer .markdown-content h5 {
  font-size: 1em;
  font-weight: 600;
}

.markdown-renderer .markdown-content h6 {
  font-size: 0.9em;
  font-weight: 600;
  color: var(--text-secondary);
}

.markdown-renderer .markdown-content p {
  margin: 0.75em 0;
  color: var(--markdown-text);
}

.markdown-renderer .markdown-content a {
  color: var(--color-accent);
  text-decoration: none;
  transition: all 0.2s ease;
}

.markdown-renderer .markdown-content a:hover {
  text-decoration: underline;
  opacity: 0.85;
}

.markdown-renderer .markdown-content ul,
.markdown-renderer .markdown-content ol {
  margin: 0.75em 0 !important;
  padding-left: 2.5em !important;
  list-style-position: outside !important;
}

.markdown-renderer .markdown-content ul {
  list-style-type: disc;
}

.markdown-renderer .markdown-content li {
  margin: 0.35em 0 !important;
  line-height: 1.7 !important;
  padding-left: 0 !important;
  text-indent: 0 !important;
  word-wrap: break-word !important;
  overflow-wrap: break-word !important;
  display: list-item !important;
}

.markdown-renderer .markdown-content li::marker {
  margin-left: 0;
}

.markdown-renderer .markdown-content li > ul,
.markdown-renderer .markdown-content li > ol {
  margin: 0.25em 0 !important;
  padding-left: 2em !important;
}

.markdown-renderer .markdown-content code {
  background: var(--markdown-inline-code-bg);
  padding: 3px 8px;
  border-radius: 6px;
  font-family: 'Fira Code', 'JetBrains Mono', 'Cascadia Code', 'SF Mono', Monaco, monospace;
  font-size: 0.875em;
  color: var(--markdown-inline-code-fg);
  border: 1px solid var(--markdown-inline-code-border);
  word-break: break-word;
}

.markdown-renderer .markdown-content .code-block-wrapper {
  margin: var(--code-block-margin-y, 0.65em) 0;
  border-radius: var(--code-block-radius, 8px);
  overflow: hidden;
  border: 1px solid var(--code-block-border);
  background: var(--code-block-shell-bg);
  box-shadow: var(--code-block-elevate-shadow, 0 1px 2px rgba(27, 31, 36, 0.06));
}

.markdown-renderer .markdown-content .code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: var(--code-header-min-height, 32px);
  padding: 0 6px 0 10px;
  background: var(--code-header-bg);
  border-bottom: 1px solid var(--code-header-border);
}

.markdown-renderer .markdown-content .code-language {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--code-lang-color);
  font-family: 'JetBrains Mono', 'Fira Code', ui-monospace, monospace;
  line-height: 1;
}

.markdown-renderer .markdown-content .copy-button {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--code-copy-btn-size, 30px);
  height: var(--code-copy-btn-size, 30px);
  margin: 2px 0;
  padding: 0;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  color: var(--code-toolbar-icon);
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.markdown-renderer .markdown-content .copy-button__icon {
  display: block;
}

.markdown-renderer .markdown-content .copy-button:hover {
  background: var(--code-copy-hover-bg);
  color: var(--code-copy-hover-fg);
}

.markdown-renderer .markdown-content .copy-button:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.markdown-renderer .markdown-content .copy-button:active {
  transform: scale(0.96);
}

.markdown-renderer .markdown-content .copy-button.copied {
  color: var(--code-copy-copied-fg);
  background: var(--code-copy-copied-bg);
  border-color: color-mix(in srgb, var(--code-copy-copied-fg) 22%, transparent);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--code-copy-copied-fg) 12%, transparent);
}

.markdown-renderer .markdown-content .code-body {
  display: flex;
  align-items: stretch;
  background: var(--code-block-bg);
}

.markdown-renderer .markdown-content .code-line-gutter {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: var(--code-gutter-padding-y, 9px) var(--code-gutter-padding-x, 7px) var(--code-gutter-padding-y, 9px)
    calc(var(--code-gutter-padding-x, 7px) + 3px);
  min-width: var(--code-gutter-min-width, 2.35rem);
  background: var(--code-gutter-bg);
  border-right: 1px solid var(--code-gutter-border);
  user-select: none;
}

.markdown-renderer .markdown-content .code-ln {
  display: block;
  width: 100%;
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', ui-monospace, monospace;
  font-size: var(--code-font-size, 12px);
  line-height: var(--code-line-height, 1.5);
  color: var(--code-gutter-text);
  font-variant-numeric: tabular-nums;
}

.markdown-renderer .markdown-content pre.code-pre {
  flex: 1;
  min-width: 0;
  margin: 0;
  padding: var(--code-block-padding-y, 9px) var(--code-block-padding-x, 11px);
  background: var(--code-block-bg);
  overflow-x: auto;
  position: relative;
  border: none;
  box-shadow: none;
  scrollbar-width: thin;
  scrollbar-color: color-mix(in srgb, var(--markdown-text) 22%, transparent) transparent;
}

.markdown-renderer .markdown-content pre.code-pre::-webkit-scrollbar {
  height: 6px;
}

.markdown-renderer .markdown-content pre.code-pre::-webkit-scrollbar-track {
  background: transparent;
}

.markdown-renderer .markdown-content pre.code-pre::-webkit-scrollbar-thumb {
  background: color-mix(in srgb, var(--markdown-text) 25%, transparent);
  border-radius: 3px;
}

.markdown-renderer .markdown-content pre.code-pre code {
  background: transparent !important;
  padding: 0 !important;
  border-radius: 0 !important;
  font-family: 'Fira Code', 'JetBrains Mono', 'Cascadia Code', 'SF Mono', Monaco, monospace;
  font-size: var(--code-font-size, 12px);
  color: var(--code-block-fg);
  line-height: var(--code-line-height, 1.5);
  border: none !important;
}

.markdown-renderer .markdown-content blockquote {
  margin: 1em 0;
  padding: 0.7em 1.1em;
  border-left: 4px solid var(--blockquote-bar);
  background: var(--blockquote-bg);
  border-radius: 0 8px 8px 0;
  color: var(--text-secondary);
}

.markdown-renderer .markdown-content blockquote p {
  margin: 0.4em 0;
}

.markdown-renderer .markdown-content table {
  width: 100%;
  margin: 1em 0;
  border-collapse: collapse;
  overflow: hidden;
  font-size: 0.95em;
  transition: background 0.15s ease;
}

.markdown-renderer .markdown-content th,
.markdown-renderer .markdown-content td {
  padding: 0.7em 1em;
  text-align: left;
  border: 1px solid var(--border-light);
}

.markdown-renderer .markdown-content th {
  background: var(--bg-secondary);
  font-weight: 600;
  color: var(--text-primary);
}

.markdown-renderer .markdown-content tr:nth-child(even) {
  background: color-mix(in srgb, var(--markdown-text) 4%, transparent);
}

.markdown-renderer .markdown-content tr:hover {
  background: color-mix(in srgb, var(--markdown-text) 7%, transparent);
}

.markdown-renderer .markdown-content hr {
  margin: 1.5em 0;
  border: none;
  height: 1px;
  background: var(--border-light);
}

.markdown-renderer .markdown-content strong,
.markdown-renderer .markdown-content b {
  font-weight: 600;
  color: var(--markdown-text);
}

.markdown-renderer .markdown-content em,
.markdown-renderer .markdown-content i {
  font-style: italic;
}

.markdown-renderer .markdown-content del {
  text-decoration: line-through;
  color: var(--text-tertiary);
}

.markdown-renderer .markdown-content img {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
}
</style>
