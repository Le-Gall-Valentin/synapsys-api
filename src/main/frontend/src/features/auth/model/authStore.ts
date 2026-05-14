import { create } from 'zustand'
import type { IAuthApi } from '../api/IAuthApi'
import {
  clearSessionHint,
  hasSessionHint,
  registerLogoutCallback,
  setAuthState,
  setSessionHint,
  triggerNavigate,
} from '@/shared/lib/authCallbacks'
import { ROUTES } from '@/shared/config/routes'
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
  return create<AuthState & AuthActions>((set) => {
    registerLogoutCallback(() => {
      clearSessionHint()
      setAuthState(false)
      set({ user: null, isAuthenticated: false, isInitializing: false })
      triggerNavigate(ROUTES.LOGIN)
    })

    return {
      user: null,
      isAuthenticated: false,
      isInitializing: true,

      async login(credentials: LoginCredentials): Promise<void> {
        const user = await api.login(credentials)
        setSessionHint()
        setAuthState(true)
        set({ user, isAuthenticated: true })
      },

      async logout(): Promise<void> {
        try {
          await api.logout()
        } finally {
          clearSessionHint()
          setAuthState(false)
          set({ user: null, isAuthenticated: false })
          triggerNavigate(ROUTES.LOGIN)
        }
      },

      async initialize(): Promise<void> {
        if (!hasSessionHint()) {
          setAuthState(false)
          set({ user: null, isAuthenticated: false, isInitializing: false })
          return
        }
        try {
          const user = await api.getMe()
          setAuthState(true)
          set({ user, isAuthenticated: true, isInitializing: false })
        } catch {
          clearSessionHint()
          setAuthState(false)
          set({ user: null, isAuthenticated: false, isInitializing: false })
        }
      },
    }
  })
}