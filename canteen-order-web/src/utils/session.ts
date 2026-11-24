import type { LoginResult, UserVO } from '@/types'

const ACCESS_KEY = 'canteen_web_access_token'
const REFRESH_KEY = 'canteen_web_refresh_token'
const USER_KEY = 'canteen_web_user'

export function getAccessToken(): string {
  return localStorage.getItem(ACCESS_KEY) || ''
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_KEY) || ''
}

export function getStoredUser(): UserVO | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserVO
  } catch {
    return null
  }
}

export function saveSession(login: LoginResult): void {
  localStorage.setItem(ACCESS_KEY, login.accessToken)
  localStorage.setItem(REFRESH_KEY, login.refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(login.user))
}

export function saveUser(user: UserVO): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession(): void {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}
