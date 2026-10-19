import { http } from '@/api/http'
import type { PageQuery, PageResult, Theme, ThemeSavePayload } from '@/api/types'

export interface AdminThemeQuery extends PageQuery {
  status?: number
}

export function listThemes(query: PageQuery = {}): Promise<PageResult<Theme>> {
  return http.get<PageResult<Theme>>('/themes', { ...query })
}

export function getTheme(id: number): Promise<Theme> {
  return http.get<Theme>(`/themes/${id}`)
}

export function adminListThemes(query: AdminThemeQuery = {}): Promise<PageResult<Theme>> {
  return http.get<PageResult<Theme>>('/admin/themes', { ...query })
}

export function createTheme(payload: ThemeSavePayload): Promise<Theme> {
  return http.post<Theme>('/admin/themes', payload)
}

export function updateTheme(id: number, payload: ThemeSavePayload): Promise<Theme> {
  return http.put<Theme>(`/admin/themes/${id}`, payload)
}

export function updateThemeStatus(id: number, status: number): Promise<Theme> {
  return http.patch<Theme>(`/admin/themes/${id}/status`, { status })
}
