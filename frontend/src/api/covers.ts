import { http } from '@/api/http'
import type { CoverUploadResult } from '@/api/types'

export function uploadCover(file: File): Promise<CoverUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  return http.upload<CoverUploadResult>('/admin/covers', formData)
}
