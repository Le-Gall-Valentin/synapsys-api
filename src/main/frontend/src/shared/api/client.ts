import axios, {
  type AxiosError,
  type InternalAxiosRequestConfig,
} from 'axios'
import { triggerLogout, isAuthenticatedState } from '@/shared/lib/authCallbacks'

interface QueueEntry {
  resolve: (value: unknown) => void
  reject: (reason: unknown) => void
  config: InternalAxiosRequestConfig
}

let isRefreshing = false
let failedQueue: QueueEntry[] = []

function flushQueue(error: unknown): void {
  failedQueue.forEach(({ resolve, reject, config }) => {
    if (error) {
      reject(error)
    } else {
      resolve(client(config))
    }
  })
  failedQueue = []
}

export const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean
    }

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    if (
      original.url === '/auth/refresh' ||
      original.url === '/auth/login' ||
      original.url === '/auth/logout'
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
      flushQueue(null)
      return client(original)
    } catch (refreshError) {
      flushQueue(refreshError)
      if (isAuthenticatedState()) {
        triggerLogout()
      }
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)