import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import type { User } from '@/entities/user'
import type { LoginCredentials } from '../model/types'
import type { IAuthApi } from './IAuthApi'
import { CredentialsError, NetworkError, ServerError } from '../model/errors'

export const authApi: IAuthApi = {
  async login(credentials: LoginCredentials): Promise<User> {
    try {
      const { data } = await client.post<User>('/auth/login', credentials)
      return data
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) throw new CredentialsError()
        if (status !== undefined && status >= 500) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async logout(): Promise<void> {
    await client.post('/auth/logout')
  },

  async getMe(): Promise<User> {
    const { data } = await client.get<User>('/auth/me')
    return data
  },
}