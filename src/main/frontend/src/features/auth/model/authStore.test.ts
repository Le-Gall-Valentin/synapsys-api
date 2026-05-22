import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createAuthStore } from './authStore'
import type { IAuthApi } from '../api/IAuthApi'
import { clearSessionHint, hasSessionHint, setSessionHint } from '@/shared/lib/sessionHint'
import { CredentialsError, NetworkError, ServerError } from './errors'

vi.mock('@/shared/lib/sessionHint', () => ({
  setSessionHint: vi.fn(),
  clearSessionHint: vi.fn(),
  hasSessionHint: vi.fn(),
}))

const mockedSetSessionHint = vi.mocked(setSessionHint)
const mockedClearSessionHint = vi.mocked(clearSessionHint)
const mockedHasSessionHint = vi.mocked(hasSessionHint)

function createApiMock(): IAuthApi {
  return {
    login: vi.fn(),
    logout: vi.fn(),
    getMe: vi.fn(),
  }
}

describe('authStore', () => {
  beforeEach(() => {
    mockedSetSessionHint.mockReset()
    mockedClearSessionHint.mockReset()
    mockedHasSessionHint.mockReset()
  })

  it('login sets authenticated state and session hint', async () => {
    const api = createApiMock()
    vi.mocked(api.login).mockResolvedValue({
      id: '1',
      username: 'user',
      role: 'USER',
    })
    const store = createAuthStore(api)

    await store.getState().login({ username: 'user', password: 'secret' })

    expect(api.login).toHaveBeenCalledWith({ username: 'user', password: 'secret' })
    expect(mockedSetSessionHint).toHaveBeenCalledTimes(1)
    expect(store.getState().isAuthenticated).toBe(true)
    expect(store.getState().user?.username).toBe('user')
  })

  it('logout clears auth state even when API fails', async () => {
    const api = createApiMock()
    vi.mocked(api.login).mockResolvedValue({
      id: '1',
      username: 'user',
      role: 'USER',
    })
    vi.mocked(api.logout).mockRejectedValue(new Error('network'))
    const store = createAuthStore(api)

    await store.getState().login({ username: 'user', password: 'secret' })
    await expect(store.getState().logout()).rejects.toThrow('network')

    expect(mockedClearSessionHint).toHaveBeenCalledTimes(1)
    expect(store.getState().isAuthenticated).toBe(false)
    expect(store.getState().user).toBeNull()
  })

  describe('initialize', () => {
    it('skips getMe and sets isInitializing=false when no session hint', async () => {
      const api = createApiMock()
      mockedHasSessionHint.mockReturnValue(false)
      const store = createAuthStore(api)

      await store.getState().initialize()

      expect(api.getMe).not.toHaveBeenCalled()
      expect(store.getState().isInitializing).toBe(false)
      expect(store.getState().isAuthenticated).toBe(false)
    })

    it('calls getMe and hydrates user when session hint is set', async () => {
      const api = createApiMock()
      mockedHasSessionHint.mockReturnValue(true)
      vi.mocked(api.getMe).mockResolvedValue({ id: '1', username: 'admin', role: 'ADMIN' })
      const store = createAuthStore(api)

      await store.getState().initialize()

      expect(api.getMe).toHaveBeenCalledTimes(1)
      expect(store.getState().isInitializing).toBe(false)
      expect(store.getState().isAuthenticated).toBe(true)
      expect(store.getState().user?.role).toBe('ADMIN')
      expect(mockedSetSessionHint).toHaveBeenCalledTimes(1)
    })

    it('clears state and hint when getMe returns 401 (session expired)', async () => {
      const api = createApiMock()
      mockedHasSessionHint.mockReturnValue(true)
      vi.mocked(api.getMe).mockRejectedValue(new CredentialsError())
      const store = createAuthStore(api)

      await store.getState().initialize()

      expect(mockedClearSessionHint).toHaveBeenCalledTimes(1)
      expect(store.getState().isInitializing).toBe(false)
      expect(store.getState().isAuthenticated).toBe(false)
      expect(store.getState().user).toBeNull()
    })

    it('keeps session hint when getMe fails with transient error (5xx or network)', async () => {
      for (const error of [new ServerError(), new NetworkError()]) {
        const api = createApiMock()
        mockedHasSessionHint.mockReturnValue(true)
        mockedClearSessionHint.mockClear()
        vi.mocked(api.getMe).mockRejectedValue(error)
        const store = createAuthStore(api)

        await store.getState().initialize()

        expect(mockedClearSessionHint).not.toHaveBeenCalled()
        expect(store.getState().isInitializing).toBe(false)
        expect(store.getState().isAuthenticated).toBe(false)
      }
    })
  })
})