import { http } from '@/api/http'
import type { PhotoItem, UploadResult } from '@/api/types'

export function listPhotos(orderId: number): Promise<PhotoItem[]> {
  return http.get<PhotoItem[]>(`/orders/${orderId}/photos`)
}

export function uploadPhotos(
  orderId: number,
  files: File[],
  onProgress?: (percent: number) => void,
): Promise<UploadResult> {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return http.upload<UploadResult>(`/admin/orders/${orderId}/photos`, formData, onProgress)
}

export function fetchPhotoBlob(previewUrl: string): Promise<Blob> {
  return http.getBlob(previewUrl).then((response) => response.data)
}

export function downloadPhoto(orderId: number, photoId: number) {
  return http.getBlob(`/orders/${orderId}/photos/${photoId}/download`)
}
