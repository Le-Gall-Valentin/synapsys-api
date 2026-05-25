import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import type { User } from '@/entities/user'
import type { LoginCredentials } from '../model/types'
import type { IAuthApi } from './IAuthApi'
import { CredentialsError, NetworkError, RateLimitError, ServerError } from '../model/errors'

export const authApi: IAuthApi = {
  async login(credentials: LoginCredentials): Promise<User> {
    try {
      const { data } = await client.post<User>('/auth/login', credentials)
      return data
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) throw new CredentialsError()
        if (status === 429) {
          const retryAfter = error.response?.headers?.['retry-after']
          const seconds = retryAfter ? (parseInt(retryAfter, 10) || null) : null
          throw new RateLimitError(seconds)
        }
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async logout(): Promise<void> {
    try {
      await client.post('/auth/logout')
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status !== undefined && status >= 500) throw new ServerError()
        if (status !== undefined) return // 4xx → token already gone, treat as success
      }
      throw new NetworkError()
    }
  },

  async getMe(): Promise<User> {
    try {
      const { data } = await client.get<User>('/auth/me')
      return data
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) throw new CredentialsError()
        if (status !== undefined && status >= 500) throw new ServerError()
        if (status !== undefined) throw new CredentialsError()
      }
      throw new NetworkError()
    }
  },
}