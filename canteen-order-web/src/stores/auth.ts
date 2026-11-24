import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { getMe } from '@/api/user'
import type { LoginPayload, LoginResult, UserVO } from '@/types'
import { clearSession, getAccessToken, getRefreshToken, getStoredUser, saveSession, saveUser } from '@/utils/session'

interface AuthState {
  accessToken: string
  refreshToken: string
  user: UserVO | null
  loading: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: getAccessToken(),
    refreshToken: getRefreshToken(),
    user: getStoredUser(),
    loading: false
  }),
  getters: {
    isLoggedIn: (state): boolean => Boolean(state.accessToken),
    role: (state): string => state.user?.roleCode || '',
    displayName: (state): string => state.user?.realName || state.user?.username || '同学',
    homePath: (state): string => {
      const role = state.user?.roleCode
      if (role === 'ADMIN') return '/admin/dashboard'
      if (role === 'STAFF') return '/staff/board'
      return '/menu'
    }
  },
  actions: {
    applySession(data: LoginResult) {
      this.accessToken = data.accessToken
      this.refreshToken = data.refreshToken
      this.user = data.user
      saveSession(data)
    },
    async login(payload: LoginPayload): Promise<LoginResult> {
      this.loading = true
      try {
        const data = await loginApi(payload)
        this.applySession(data)
        return data
      } finally {
        this.loading = false
      }
    },
    async fetchMe(): Promise<UserVO> {
      const user = await getMe()
      this.user = user
      this.accessToken = getAccessToken()
      saveUser(user)
      return user
    },
    async logout(): Promise<void> {
      try {
        await logoutApi()
      } catch {
        // 后端不可用时也要允许本地退出
      } finally {
        this.reset()
        clearSession()
      }
    },
    reset() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
    }
  }
})
