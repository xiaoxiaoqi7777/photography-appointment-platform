import type { AxiosResponse } from 'axios'

export function extractFilename(disposition: string | undefined, fallback: string): string {
  if (!disposition) {
    return fallback
  }
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (encoded?.[1]) {
    try {
      return decodeURIComponent(encoded[1])
    } catch {
      return fallback
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain?.[1] ?? fallback
}

export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function saveBlobResponse(response: AxiosResponse<Blob>, fallbackName: string): void {
  const filename = extractFilename(response.headers['content-disposition'], fallbackName)
  saveBlob(response.data, filename)
}
