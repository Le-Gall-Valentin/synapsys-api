import { client } from '@/shared/api'
import type { User } from '@/entities/user'
import type { LoginCredentials } from '../model/types'
import type { IAuthApi } from './IAuthApi'

export const authApi: IAuthApi = {
  async login(credentials: LoginCredentials): Promise<User> {
    const { data } = await client.post<User>('/auth/login', credentials)
    return data
  },

  async logout(): Promise<void> {
    await client.post('/auth/logout')
  },

  async getMe(): Promise<User> {
    const { data } = await client.get<User>('/auth/me')
    return data
  },
}
