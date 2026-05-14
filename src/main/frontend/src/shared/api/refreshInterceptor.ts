import type { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { triggerLogout } from '@/shared/lib/authCallbacks'

interface QueueEntry {
  resolve: (value: unknown) => void
  reject: (reason: unknown) => void
  config: InternalAxiosRequestConfig
}

let isRefreshing = false
let failedQueue: QueueEntry[] = []

const AUTH_URLS = ['/auth/refresh', '/auth/login', '/auth/logout']

function flushQueue(error: unknown, client: AxiosInstance): void {
  failedQueue.forEach(({ resolve, reject, config }) => {
    if (error) reject(error)
    else resolve(client(config))
  })
  failedQueue = []
}

export function attachRefreshInterceptor(client: AxiosInstance): void {
  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

      if (error.response?.status !== 401 || original._retry) {
        return Promise.reject(error)
      }

      if (AUTH_URLS.some((url) => original.url === url)) {
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
        flushQueue(null, client)
        return client(original)
      } catch (refreshError) {
        flushQueue(refreshError, client)
        triggerLogout()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    },
  )
}