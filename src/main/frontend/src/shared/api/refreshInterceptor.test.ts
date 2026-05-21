import axios, {AxiosError, type InternalAxiosRequestConfig} from 'axios'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {attachRefreshInterceptor} from './refreshInterceptor'
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
    attachRefreshInterceptor(instance)
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

    const handler = (instance.interceptors.response as unknown as { handlers: { rejected: (e: unknown) => Promise<unknown> }[] }).handlers[0]
    await expect(handler.rejected(error)).rejects.toBeDefined()
    expect(mockedTriggerSessionExpired).not.toHaveBeenCalled()
  })

  it('lets 401 on /auth/login pass through without refreshing', async () => {
    const instance = axios.create()
    let refreshCalled = false
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCalled = true
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    attachRefreshInterceptor(instance)

    const error = make401('/auth/login')
    const handler = (instance.interceptors.response as unknown as { handlers: { rejected: (e: unknown) => Promise<unknown> }[] }).handlers[0]
    await expect(handler.rejected(error)).rejects.toBeDefined()
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
    attachRefreshInterceptor(instance)

    const error = make401('/auth/refresh')
    const handler = (instance.interceptors.response as unknown as { handlers: { rejected: (e: unknown) => Promise<unknown> }[] }).handlers[0]
    await expect(handler.rejected(error)).rejects.toBeDefined()
    expect(refreshCallCount).toBe(0)
  })

  it('does not retry requests that already have _retry=true', async () => {
    const instance = axios.create()
    let refreshCalled = false
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCalled = true
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    attachRefreshInterceptor(instance)

    const error = make401('/api/protected', true)
    const handler = (instance.interceptors.response as unknown as { handlers: { rejected: (e: unknown) => Promise<unknown> }[] }).handlers[0]
    await expect(handler.rejected(error)).rejects.toBeDefined()
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
    attachRefreshInterceptor(instance)

    const error = make401('/api/protected')
    const handler = (instance.interceptors.response as unknown as { handlers: { rejected: (e: unknown) => Promise<unknown> }[] }).handlers[0]
    await expect(handler.rejected(error)).rejects.toBeDefined()
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

    attachRefreshInterceptor(instance)

    const handler = (
      instance.interceptors.response as unknown as {
        handlers: { rejected: (e: unknown) => Promise<unknown> }[]
      }
    ).handlers[0]

    const [, ,] = await Promise.all([
      handler.rejected(make401('/api/data1')),
      handler.rejected(make401('/api/data2')),
      handler.rejected(make401('/api/data3')),
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

    attachRefreshInterceptor(instance)

    const handler = (
      instance.interceptors.response as unknown as {
        handlers: { rejected: (e: unknown) => Promise<unknown> }[]
      }
    ).handlers[0]

    await Promise.allSettled([
      handler.rejected(make401('/api/data1')),
      handler.rejected(make401('/api/data2')),
      handler.rejected(make401('/api/data3')),
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
    attachRefreshInterceptor(instance)

    const error = make401('/api/data')
    const handler = (instance.interceptors.response as unknown as { handlers: { rejected: (e: unknown) => Promise<unknown> }[] }).handlers[0]
    const result = await handler.rejected(error)
    expect((result as { data: { ok: boolean } }).data.ok).toBe(true)
    expect(retried).toContain('/api/data')
    expect(mockedTriggerSessionExpired).not.toHaveBeenCalled()
  })
})