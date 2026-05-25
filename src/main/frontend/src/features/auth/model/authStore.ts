import { create } from 'zustand'
import type { IAuthApi } from './IAuthApi'
import { clearSessionHint, hasSessionHint, setSessionHint } from '@/shared/lib'
import type { User } from '@/entities/user'
import type { LoginCredentials } from './types'
import { CredentialsError } from './errors'

export interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isInitializing: boolean
}

export interface AuthActions {
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => Promise<void>
  initialize: (signal?: AbortSignal) => Promise<void>
}

export function createAuthStore(api: IAuthApi) {
  let initializationStarted = false

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

    async initialize(signal?: AbortSignal): Promise<void> {
      if (initializationStarted) return
      initializationStarted = true

      if (!hasSessionHint()) {
        set({ isInitializing: false })
        return
      }
      try {
        const user = await api.getMe()
        if (signal?.aborted) return
        setSessionHint()
        set({ user, isAuthenticated: true, isInitializing: false })
      } catch (error) {
        if (signal?.aborted) return
        // Only invalidate the session on actual auth failure (401); leave hint intact on transient errors
        if (error instanceof CredentialsError) clearSessionHint()
        set({ user: null, isAuthenticated: false, isInitializing: false })
      }
    },
  }))
}