export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

export function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function statusLabel(status: string): string {
  const map: Record<string, string> = {
    UPLOADING: '上传中',
    PARSING: '解析中',
    INDEXING: '索引中',
    READY: '就绪',
    ERROR: '错误'
  }
  return map[status] || status
}
