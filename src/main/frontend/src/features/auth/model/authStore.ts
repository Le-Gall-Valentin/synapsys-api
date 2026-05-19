import { create } from 'zustand'
import { isAxiosError } from 'axios'
import type { IAuthApi } from '../api/IAuthApi'
import { clearSessionHint, setSessionHint } from '@/shared/lib'
import type { UserDTO } from '@/entities/user'
import type { LoginCredentials } from './types'
import { CredentialsError, NetworkError } from './errors'

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
      try {
        const user = await api.login(credentials)
        setSessionHint()
        set({ user, isAuthenticated: true })
      } catch (error) {
        if (isAxiosError(error) && error.response?.status === 401) throw new CredentialsError()
        throw new NetworkError()
      }
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