import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createAuthStore } from './authStore'
import type { IAuthApi } from '../api/IAuthApi'
import { clearSessionHint, setSessionHint } from '@/shared/lib/sessionHint'

vi.mock('@/shared/lib/sessionHint', () => ({
  setSessionHint: vi.fn(),
  clearSessionHint: vi.fn(),
  hasSessionHint: vi.fn(),
}))

const mockedSetSessionHint = vi.mocked(setSessionHint)
const mockedClearSessionHint = vi.mocked(clearSessionHint)

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

  it('initialize always calls getMe regardless of hint', async () => {
    const api = createApiMock()
    vi.mocked(api.getMe).mockResolvedValue({ id: '1', username: 'admin', role: 'ADMIN' })
    const store = createAuthStore(api)

    await store.getState().initialize()

    expect(api.getMe).toHaveBeenCalledTimes(1)
    expect(store.getState().isAuthenticated).toBe(true)
    expect(mockedSetSessionHint).toHaveBeenCalledTimes(1)
  })

  it('initialize hydrates user on success', async () => {
    const api = createApiMock()
    vi.mocked(api.getMe).mockResolvedValue({
      id: '1',
      username: 'admin',
      role: 'ADMIN',
    })
    const store = createAuthStore(api)

    await store.getState().initialize()

    expect(store.getState().isInitializing).toBe(false)
    expect(store.getState().isAuthenticated).toBe(true)
    expect(store.getState().user?.role).toBe('ADMIN')
  })

  it('initialize clears state and hint when getMe fails', async () => {
    const api = createApiMock()
    vi.mocked(api.getMe).mockRejectedValue(new Error('401'))
    const store = createAuthStore(api)

    await store.getState().initialize()

    expect(mockedClearSessionHint).toHaveBeenCalledTimes(1)
    expect(store.getState().isInitializing).toBe(false)
    expect(store.getState().isAuthenticated).toBe(false)
    expect(store.getState().user).toBeNull()
  })
})