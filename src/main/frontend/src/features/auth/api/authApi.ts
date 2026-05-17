import { client } from '@/shared/api'
import type { UserDTO } from '@/entities/user'
import type { LoginCredentials } from '../model/types'
import type { IAuthApi } from './IAuthApi'

export const authApi: IAuthApi = {
  async login(credentials: LoginCredentials): Promise<UserDTO> {
    await client.post('/auth/login', credentials)
    const { data } = await client.get<UserDTO>('/auth/me')
    return data
  },

  async logout(): Promise<void> {
    await client.post('/auth/logout')
  },

  async getMe(): Promise<UserDTO> {
    const { data } = await client.get<UserDTO>('/auth/me')
    return data
  },
}
