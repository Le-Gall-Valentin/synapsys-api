import type { AxiosInstance, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { triggerSessionExpired } from '@/shared/lib'

interface QueueEntry {
  resolve: (value: unknown) => void
  reject: (reason: unknown) => void
  config: InternalAxiosRequestConfig
}

export function createRefreshInterceptorHandlers(client: AxiosInstance): {
  onFulfilled: (response: AxiosResponse) => AxiosResponse
  onRejected: (error: AxiosError) => Promise<unknown>
} {
  // State local to this instance — no pollution between calls
  let isRefreshing = false
  let failedQueue: QueueEntry[] = []
  let sessionExpiredTriggered = false

  function flushQueue(error: unknown): void {
    failedQueue.forEach(({ resolve, reject, config }) => {
      if (error) reject(error)
      else {
        config._retry = true
        resolve(client(config))
      }
    })
    failedQueue = []
  }

  function onFulfilled(response: AxiosResponse): AxiosResponse {
    const url = response.config.url?.split('?')[0]
    if (url?.endsWith('/auth/login')) {
      sessionExpiredTriggered = false
    }
    return response
  }

  async function onRejected(error: AxiosError): Promise<unknown> {
    if (!error.config) {
      return Promise.reject(error)
    }

    const original = error.config as InternalAxiosRequestConfig

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    const requestPath = original.url?.split('?')[0]
    if (
      requestPath?.endsWith('/auth/login') ||
      requestPath?.endsWith('/auth/refresh') ||
      requestPath?.endsWith('/auth/logout')
    ) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject, config: original })
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      await client.post('/auth/refresh')
      sessionExpiredTriggered = false
      flushQueue(null)
      return client(original)
    } catch (refreshError) {
      flushQueue(refreshError)
      if (!sessionExpiredTriggered) {
        sessionExpiredTriggered = true
        triggerSessionExpired()
      }
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }

  return { onFulfilled, onRejected }
}

export function attachRefreshInterceptor(client: AxiosInstance): void {
  const { onFulfilled, onRejected } = createRefreshInterceptorHandlers(client)
  client.interceptors.response.use(onFulfilled, onRejected)
}
