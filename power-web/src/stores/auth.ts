import { defineStore } from 'pinia'
import { fetchMe, fetchMenuTree, loginWeb, logout as apiLogout } from '@/api/auth'
import type { CurrentUser, MenuVO } from '@/types/api'
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setTokens,
} from '@/utils/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as CurrentUser | null,
    menus: [] as MenuVO[],
    routesLoaded: false,
  }),

  getters: {
    isLoggedIn: () => !!getAccessToken(),
    authorities: (state) => state.user?.authorities ?? [],
    displayName: (state) => state.user?.nickname || state.user?.username || '',
  },

  actions: {
    async login(username: string, password: string) {
      const data = await loginWeb(username, password)
      setTokens(data.accessToken, data.refreshToken)
      await this.loadUserContext()
    },

    async loadUserContext() {
      this.user = await fetchMe()
      this.menus = await fetchMenuTree()
      this.routesLoaded = false
    },

    async refreshUser() {
      this.user = await fetchMe()
    },

    async logout() {
      const accessToken = getAccessToken()
      const refreshToken = getRefreshToken()
      try {
        if (accessToken || refreshToken) {
          await apiLogout(accessToken ?? undefined, refreshToken ?? undefined)
        }
      } catch {
        // 登出接口失败也继续清理本地态
      }
      this.reset()
    },

    reset() {
      clearTokens()
      this.user = null
      this.menus = []
      this.routesLoaded = false
    },

    markRoutesLoaded() {
      this.routesLoaded = true
    },
  },
})
