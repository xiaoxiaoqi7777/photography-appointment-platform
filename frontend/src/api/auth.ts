import { http } from '@/api/http'
import type { LoginPayload, LoginResult, RegisterPayload } from '@/api/types'

export function login(payload: LoginPayload): Promise<LoginResult> {
  return http.post<LoginResult>('/auth/login', payload)
}

export function register(payload: RegisterPayload): Promise<null> {
  return http.post<null>('/auth/register', payload)
}
