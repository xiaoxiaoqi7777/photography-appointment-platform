export const TOKEN_KEY = 'photography.token'
export const ROLE_KEY = 'photography.role'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function getStoredRole(): string {
  return localStorage.getItem(ROLE_KEY) ?? ''
}

export function saveAuth(token: string, role: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(ROLE_KEY, role)
}

export function saveRole(role: string): void {
  localStorage.setItem(ROLE_KEY, role)
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
}
