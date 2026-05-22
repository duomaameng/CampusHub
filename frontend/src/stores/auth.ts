import { defineStore } from 'pinia'

import { authApi, notificationApi, userApi } from '@/services/api'
import type { LoginUser, UserProfile } from '@/types'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('campus-hub-token') || '',
    user: JSON.parse(localStorage.getItem('campus-hub-user') || 'null') as LoginUser | null,
    profile: null as UserProfile | null,
    unreadCount: 0
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token && state.user),
    isAdmin: (state) => state.user?.role === 'ADMIN'
  },
  actions: {
    async login(email: string, password: string) {
      const result = await authApi.login(email, password)
      this.token = result.token
      this.user = result.user
      localStorage.setItem('campus-hub-token', result.token)
      localStorage.setItem('campus-hub-user', JSON.stringify(result.user))
      await this.loadMe()
      await this.refreshUnread()
    },
    async register(email: string, password: string, confirmPassword: string, code: string) {
      return authApi.register(email, password, confirmPassword, code)
    },
    async sendVerificationCode(email: string, purpose: 'REGISTER' | 'RESET_PASSWORD') {
      return authApi.sendVerificationCode(email, purpose)
    },
    async verifyEmail(email: string, code: string) {
      const result = await authApi.verifyEmail(email, code)
      if (this.user?.email === email || this.profile?.email === email) {
        await this.loadMe()
      }
      return result
    },
    async resetPassword(email: string, code: string, newPassword: string, confirmNewPassword: string) {
      return authApi.resetPassword(email, code, newPassword, confirmNewPassword)
    },
    async logout() {
      await authApi.logout()
      this.token = ''
      this.user = null
      this.profile = null
      this.unreadCount = 0
      localStorage.removeItem('campus-hub-token')
      localStorage.removeItem('campus-hub-user')
    },
    async loadMe() {
      if (!this.token) return
      this.profile = await userApi.me()
      this.user = {
        id: this.profile.id,
        email: this.profile.email,
        role: this.profile.role,
        status: this.profile.status,
        verified: this.profile.verified,
        nickname: this.profile.profile.nickname,
        avatarUrl: this.profile.profile.avatarUrl
      }
      localStorage.setItem('campus-hub-user', JSON.stringify(this.user))
    },
    async refreshUnread() {
      if (!this.token) return
      const result = await notificationApi.unreadCount()
      this.unreadCount = result.count
    }
  }
})
