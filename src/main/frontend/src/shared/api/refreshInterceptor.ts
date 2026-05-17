import type { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { triggerLogout } from '@/shared/lib/authCallbacks'

interface QueueEntry {
  resolve: (value: unknown) => void
  reject: (reason: unknown) => void
  config: InternalAxiosRequestConfig
}

export function attachRefreshInterceptor(client: AxiosInstance): void {
  // State local to this instance — no pollution between calls
  let isRefreshing = false
  let failedQueue: QueueEntry[] = []

  function flushQueue(error: unknown): void {
    failedQueue.forEach(({ resolve, reject, config }) => {
      if (error) reject(error)
      else resolve(client(config))
    })
    failedQueue = []
  }

  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

      if (error.response?.status !== 401 || original._retry) {
        return Promise.reject(error)
      }

      // Any URL under /auth/ should not trigger a refresh retry
      if (original.url?.startsWith('/auth/')) {
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
        flushQueue(null)
        return client(original)
      } catch (refreshError) {
        flushQueue(refreshError)
        triggerLogout()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    },
  )
}