import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import type { LoginCredentials } from '../model/types'
import { authApi } from './authApi'
import { client } from '@/shared/api'
import { CredentialsError, NetworkError, RateLimitError, ServerError } from '../model/errors'

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

  it('login throws CredentialsError on 401', async () => {
    const err = new axios.AxiosError('Unauthorized', undefined, undefined, undefined, {
      status: 401, data: {}, headers: {}, config: {} as never, statusText: 'Unauthorized',
    })
    mockedClient.post.mockRejectedValue(err)
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(CredentialsError)
  })

  it('login throws ServerError on 500', async () => {
    const err = new axios.AxiosError('Internal Server Error', undefined, undefined, undefined, {
      status: 500, data: {}, headers: {}, config: {} as never, statusText: 'Internal Server Error',
    })
    mockedClient.post.mockRejectedValue(err)
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(ServerError)
  })

  it('login throws RateLimitError on 429', async () => {
    const err = new axios.AxiosError('Too Many Requests', undefined, undefined, undefined, {
      status: 429, data: {}, headers: {}, config: {} as never, statusText: 'Too Many Requests',
    })
    mockedClient.post.mockRejectedValue(err)
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(RateLimitError)
  })

  it('login throws NetworkError when no response', async () => {
    mockedClient.post.mockRejectedValue(new Error('Network Error'))
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(NetworkError)
  })

  it('logout throws ServerError on 500', async () => {
    const err = new axios.AxiosError('Internal Server Error', undefined, undefined, undefined, {
      status: 500, data: {}, headers: {}, config: {} as never, statusText: 'Internal Server Error',
    })
    mockedClient.post.mockRejectedValue(err)
    await expect(authApi.logout()).rejects.toBeInstanceOf(ServerError)
  })

  it('logout succeeds silently on 4xx (token already gone)', async () => {
    const err = new axios.AxiosError('Unauthorized', undefined, undefined, undefined, {
      status: 401, data: {}, headers: {}, config: {} as never, statusText: 'Unauthorized',
    })
    mockedClient.post.mockRejectedValue(err)
    await expect(authApi.logout()).resolves.toBeUndefined()
  })

  it('logout throws NetworkError when no response', async () => {
    mockedClient.post.mockRejectedValue(new Error('Network Error'))
    await expect(authApi.logout()).rejects.toBeInstanceOf(NetworkError)
  })

  it('getMe throws CredentialsError on 401', async () => {
    const err = new axios.AxiosError('Unauthorized', undefined, undefined, undefined, {
      status: 401, data: {}, headers: {}, config: {} as never, statusText: 'Unauthorized',
    })
    mockedClient.get.mockRejectedValue(err)
    await expect(authApi.getMe()).rejects.toBeInstanceOf(CredentialsError)
  })

  it('getMe throws ServerError on 500', async () => {
    const err = new axios.AxiosError('Internal Server Error', undefined, undefined, undefined, {
      status: 500, data: {}, headers: {}, config: {} as never, statusText: 'Internal Server Error',
    })
    mockedClient.get.mockRejectedValue(err)
    await expect(authApi.getMe()).rejects.toBeInstanceOf(ServerError)
  })

  it('getMe throws NetworkError when no response', async () => {
    mockedClient.get.mockRejectedValue(new Error('Network Error'))
    await expect(authApi.getMe()).rejects.toBeInstanceOf(NetworkError)
  })

  it('login throws ServerError on 400 (validation error)', async () => {
    const err = new axios.AxiosError('Bad Request', undefined, undefined, undefined, {
      status: 400, data: {}, headers: {}, config: {} as never, statusText: 'Bad Request',
    })
    mockedClient.post.mockRejectedValue(err)
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(ServerError)
  })

  it('login throws ServerError on 403', async () => {
    const err = new axios.AxiosError('Forbidden', undefined, undefined, undefined, {
      status: 403, data: {}, headers: {}, config: {} as never, statusText: 'Forbidden',
    })
    mockedClient.post.mockRejectedValue(err)
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(ServerError)
  })

  it('login RateLimitError carries retryAfterSeconds from header', async () => {
    const err = new axios.AxiosError('Too Many Requests', undefined, undefined, undefined, {
      status: 429, data: {}, headers: { 'retry-after': '45' }, config: {} as never, statusText: 'Too Many Requests',
    })
    mockedClient.post.mockRejectedValue(err)
    const caught = await authApi.login({ username: 'u', password: 'p' }).catch((e) => e)
    expect(caught).toBeInstanceOf(RateLimitError)
    expect((caught as RateLimitError).retryAfterSeconds).toBe(45)
  })

  it('login RateLimitError has null retryAfterSeconds when header absent', async () => {
    const err = new axios.AxiosError('Too Many Requests', undefined, undefined, undefined, {
      status: 429, data: {}, headers: {}, config: {} as never, statusText: 'Too Many Requests',
    })
    mockedClient.post.mockRejectedValue(err)
    const caught = await authApi.login({ username: 'u', password: 'p' }).catch((e) => e)
    expect(caught).toBeInstanceOf(RateLimitError)
    expect((caught as RateLimitError).retryAfterSeconds).toBeNull()
  })

  it('getMe throws CredentialsError on 403', async () => {
    const err = new axios.AxiosError('Forbidden', undefined, undefined, undefined, {
      status: 403, data: {}, headers: {}, config: {} as never, statusText: 'Forbidden',
    })
    mockedClient.get.mockRejectedValue(err)
    await expect(authApi.getMe()).rejects.toBeInstanceOf(CredentialsError)
  })
})
