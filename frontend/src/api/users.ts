import { http } from '@/api/http'
import type { UpdateMePayload, UserProfile } from '@/api/types'

export function getMe(): Promise<UserProfile> {
  return http.get<UserProfile>('/users/me')
}

export function updateMe(payload: UpdateMePayload): Promise<UserProfile> {
  return http.put<UserProfile>('/users/me', payload)
}
