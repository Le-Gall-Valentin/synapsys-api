import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { LoginCredentials } from '../model/types'
import { authApi } from './authApi'
import { client } from '@/shared/api'

vi.mock('@/shared/api', () => ({
  client: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

const mockedClient = client as unknown as {
  post: ReturnType<typeof vi.fn>
  get: ReturnType<typeof vi.fn>
}

describe('authApi', () => {
  beforeEach(() => {
    mockedClient.post.mockReset()
    mockedClient.get.mockReset()
  })

  it('login posts credentials and returns user from response', async () => {
    const credentials: LoginCredentials = { username: 'user', password: 'secret' }
    const user = { id: '1', username: 'user', role: 'USER' }
    mockedClient.post.mockResolvedValue({ data: user })

    await expect(authApi.login(credentials)).resolves.toEqual(user)
    expect(mockedClient.post).toHaveBeenCalledWith('/auth/login', credentials)
    expect(mockedClient.get).not.toHaveBeenCalled()
  })

  it('logout calls logout endpoint', async () => {
    mockedClient.post.mockResolvedValue({})

    await authApi.logout()

    expect(mockedClient.post).toHaveBeenCalledWith('/auth/logout')
    expect(mockedClient.post).toHaveBeenCalledTimes(1)
  })

  it('getMe reads current user endpoint', async () => {
    const user = { id: '1', username: 'user', role: 'ADMIN' }
    mockedClient.get.mockResolvedValue({ data: user })

    await expect(authApi.getMe()).resolves.toEqual(user)
    expect(mockedClient.get).toHaveBeenCalledWith('/auth/me')
  })
})
