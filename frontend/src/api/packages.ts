import { http } from '@/api/http'
import type {
  PackageSavePayload,
  PageQuery,
  PageResult,
  PhotoPackage,
} from '@/api/types'

export interface AdminPackageQuery extends PageQuery {
  themeId?: number
  status?: number
}

export function listPackagesByTheme(themeId: number): Promise<PhotoPackage[]> {
  return http.get<PhotoPackage[]>(`/themes/${themeId}/packages`)
}

export function getPackage(id: number): Promise<PhotoPackage> {
  return http.get<PhotoPackage>(`/packages/${id}`)
}

export function adminListPackages(query: AdminPackageQuery = {}): Promise<PageResult<PhotoPackage>> {
  return http.get<PageResult<PhotoPackage>>('/admin/packages', { ...query })
}

export function createPackage(payload: PackageSavePayload): Promise<PhotoPackage> {
  return http.post<PhotoPackage>('/admin/packages', payload)
}

export function updatePackage(id: number, payload: PackageSavePayload): Promise<PhotoPackage> {
  return http.put<PhotoPackage>(`/admin/packages/${id}`, payload)
}

export function updatePackageStatus(id: number, status: number): Promise<PhotoPackage> {
  return http.patch<PhotoPackage>(`/admin/packages/${id}/status`, { status })
}
