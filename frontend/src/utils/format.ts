export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || Number.isNaN(Number(bytes))) {
    return '-'
  }
  const size = Number(bytes)
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

export function formatPrice(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const price = Number(value)
  return Number.isFinite(price) ? price.toFixed(2) : String(value)
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ')
}

export function todayStart(): Date {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  return date
}

export function queryString(value: unknown): string {
  if (Array.isArray(value)) {
    return value.length > 0 ? String(value[0]) : ''
  }
  return typeof value === 'string' ? value : ''
}
