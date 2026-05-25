import axios, {AxiosError, type InternalAxiosRequestConfig} from 'axios'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {createRefreshInterceptorHandlers} from './refreshInterceptor'
import {triggerSessionExpired} from '@/shared/lib'

vi.mock('@/shared/lib', () => ({
  triggerSessionExpired: vi.fn(),
  setSessionExpiredCallback: vi.fn(),
  setSessionHint: vi.fn(),
  clearSessionHint: vi.fn(),
  hasSessionHint: vi.fn(),
}))

const mockedTriggerSessionExpired = vi.mocked(triggerSessionExpired)

function make401(url: string, retry = false): AxiosError {
  const config = {
    url,
    headers: axios.defaults.headers as never,
    _retry: retry,
  } as InternalAxiosRequestConfig & { _retry: boolean }
  return new AxiosError('Unauthorized', '401', config, null, {
    status: 401,
    data: null,
    headers: {} as never,
    config,
    statusText: 'Unauthorized',
  })
}

describe('refreshInterceptor', () => {
  beforeEach(() => {
    mockedTriggerSessionExpired.mockReset()
  })

  it('lets non-401 errors pass through untouched', async () => {
    const instance = axios.create()
    const { onRejected } = createRefreshInterceptorHandlers(instance)
    const config = {
      url: '/api/data',
      headers: {} as never,
    } as InternalAxiosRequestConfig
    const error = new AxiosError('Server Error', '500', config, null, {
      status: 500,
      data: null,
      headers: {} as never,
      config,
      statusText: 'Internal Server Error',
    })

    await expect(onRejected(error)).rejects.toBeDefined()
    expect(mockedTriggerSessionExpired).not.toHaveBeenCalled()
  })

  it('lets 401 on /auth/login pass through without refreshing', async () => {
    const instance = axios.create()
    let refreshCalled = false
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCalled = true
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance)

    const error = make401('/auth/login')
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(refreshCalled).toBe(false)
    expect(mockedTriggerSessionExpired).not.toHaveBeenCalled()
  })

  it('lets 401 on /auth/refresh pass through without another refresh', async () => {
    const instance = axios.create()
    let refreshCallCount = 0
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCallCount++
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance)

    const error = make401('/auth/refresh')
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(refreshCallCount).toBe(0)
  })

  it('does not retry requests that already have _retry=true', async () => {
    const instance = axios.create()
    let refreshCalled = false
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCalled = true
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance)

    const error = make401('/api/protected', true)
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(refreshCalled).toBe(false)
    expect(mockedTriggerSessionExpired).not.toHaveBeenCalled()
  })

  it('triggers logout when refresh fails on 401', async () => {
    const instance = axios.create()
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        const refreshConfig = { url: '/auth/refresh', headers: {} as never } as InternalAxiosRequestConfig
        throw new AxiosError('Unauthorized', '401', refreshConfig, null, {
          status: 401,
          data: null,
          headers: {} as never,
          config: refreshConfig,
          statusText: 'Unauthorized',
        })
      }
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance)

    const error = make401('/api/protected')
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(mockedTriggerSessionExpired).toHaveBeenCalledTimes(1)
  })

  it('queues concurrent 401s and retries all after successful refresh', async () => {
    const instance = axios.create()
    let refreshCallCount = 0
    const retried: string[] = []

    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshCallCount++
        return { data: {}, status: 204, statusText: 'No Content', headers: {}, config }
      }
      retried.push(config.url!)
      return { data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config }
    }

    const { onRejected } = createRefreshInterceptorHandlers(instance)

    const [, ,] = await Promise.all([
      onRejected(make401('/api/data1')),
      onRejected(make401('/api/data2')),
      onRejected(make401('/api/data3')),
    ])

    expect(refreshCallCount).toBe(1)
    expect(retried).toContain('/api/data1')
    expect(retried).toContain('/api/data2')
    expect(retried).toContain('/api/data3')
    expect(mockedTriggerSessionExpired).not.toHaveBeenCalled()
  })

  it('triggers session expired only once when concurrent 401s arrive after failed refresh', async () => {
    const instance = axios.create()
    let refreshCallCount = 0

    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshCallCount++
        const refreshConfig = { url: '/auth/refresh', headers: {} as never } as InternalAxiosRequestConfig
        throw new AxiosError('Unauthorized', '401', refreshConfig, null, {
          status: 401,
          data: null,
          headers: {} as never,
          config: refreshConfig,
          statusText: 'Unauthorized',
        })
      }
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }

    const { onRejected } = createRefreshInterceptorHandlers(instance)

    await Promise.allSettled([
      onRejected(make401('/api/data1')),
      onRejected(make401('/api/data2')),
      onRejected(make401('/api/data3')),
    ])

    expect(refreshCallCount).toBe(1)
    expect(mockedTriggerSessionExpired).toHaveBeenCalledTimes(1)
  })

  it('retries original request after successful refresh', async () => {
    const instance = axios.create()
    const retried: string[] = []
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        return { data: {}, status: 204, statusText: 'No Content', headers: {}, config }
      }
      if (config.url === '/api/data') {
        retried.push(config.url)
        return { data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config }
      }
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance)

    const error = make401('/api/data')
    const result = await onRejected(error)
    expect((result as { data: { ok: boolean } }).data.ok).toBe(true)
    expect(retried).toContain('/api/data')
    expect(mockedTriggerSessionExpired).not.toHaveBeenCalled()
  })

  it('resets sessionExpiredTriggered after successful login so next expiry triggers redirect', async () => {
    const instance = axios.create()
    let refreshCallCount = 0

    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshCallCount++
        const refreshConfig = { url: '/auth/refresh', headers: {} as never } as InternalAxiosRequestConfig
        throw new AxiosError('Unauthorized', '401', refreshConfig, null, {
          status: 401, data: null, headers: {} as never, config: refreshConfig, statusText: 'Unauthorized',
        })
      }
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }

    const { onFulfilled, onRejected } = createRefreshInterceptorHandlers(instance)
    instance.interceptors.response.use(onFulfilled, onRejected)

    // First 401 — refresh fails — sessionExpiredTriggered = true
    await expect(onRejected(make401('/api/data1'))).rejects.toBeDefined()
    expect(mockedTriggerSessionExpired).toHaveBeenCalledTimes(1)

    // Simulate re-login (successful POST to /auth/login)
    await instance.post('/auth/login')

    // Second 401 — sessionExpiredTriggered must have been reset — must trigger again
    await expect(onRejected(make401('/api/data2'))).rejects.toBeDefined()
    expect(mockedTriggerSessionExpired).toHaveBeenCalledTimes(2)
  })
})