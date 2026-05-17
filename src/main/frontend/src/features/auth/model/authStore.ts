import { create } from 'zustand'
import type { IAuthApi } from '../api/IAuthApi'
import { clearSessionHint, setSessionHint } from '@/shared/lib/sessionHint'
import type { UserDTO } from '@/entities/user'
import type { LoginCredentials } from './types'

interface AuthState {
  user: UserDTO | null
  isAuthenticated: boolean
  isInitializing: boolean
}

interface AuthActions {
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => Promise<void>
  initialize: () => Promise<void>
}

export function createAuthStore(api: IAuthApi) {
  return create<AuthState & AuthActions>((set) => ({
    user: null,
    isAuthenticated: false,
    isInitializing: true,

    async login(credentials: LoginCredentials): Promise<void> {
      const user = await api.login(credentials)
      setSessionHint()
      set({ user, isAuthenticated: true })
    },

    async logout(): Promise<void> {
      try {
        await api.logout()
      } finally {
        clearSessionHint()
        set({ user: null, isAuthenticated: false })
      }
    },

    async initialize(): Promise<void> {
      try {
        const user = await api.getMe()
        setSessionHint()
        set({ user, isAuthenticated: true, isInitializing: false })
      } catch {
        clearSessionHint()
        set({ user: null, isAuthenticated: false, isInitializing: false })
      }
    },
  }))
}