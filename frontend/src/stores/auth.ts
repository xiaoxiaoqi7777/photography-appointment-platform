import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
import { getMe } from '@/api/users'
import type { LoginPayload, LoginResult, Role, UserProfile } from '@/api/types'
import { clearAuth, getStoredRole, getToken, saveAuth, saveRole } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const role = ref<Role | ''>((getStoredRole() as Role | '') || '')
  const user = ref<UserProfile | null>(null)
  const loadingProfile = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isUser = computed(() => role.value === 'USER')
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  function applyUser(profile: UserProfile): void {
    user.value = profile
    role.value = profile.role
    saveRole(profile.role)
  }

  async function login(payload: LoginPayload): Promise<LoginResult> {
    const result = await loginApi(payload)
    token.value = result.token
    role.value = result.role
    saveAuth(result.token, result.role)
    try {
      await loadProfile(true)
    } catch (error) {
      logout()
      throw error
    }
    return result
  }

  async function loadProfile(force = false): Promise<UserProfile> {
    if (user.value && !force) {
      return user.value
    }
    loadingProfile.value = true
    try {
      const profile = await getMe()
      applyUser(profile)
      return profile
    } finally {
      loadingProfile.value = false
    }
  }

  function setUser(profile: UserProfile): void {
    applyUser(profile)
  }

  function logout(): void {
    token.value = ''
    role.value = ''
    user.value = null
    clearAuth()
  }

  return {
    token,
    role,
    user,
    loadingProfile,
    isLoggedIn,
    isAdmin,
    isUser,
    displayName,
    login,
    loadProfile,
    setUser,
    logout,
  }
})
