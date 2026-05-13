import { client } from '@/shared/api'
import type { UserDTO } from '@/entities/user'
import type { LoginCredentials } from '../model/types'
import type { IAuthApi } from './IAuthApi'

export const authApi: IAuthApi = {
  async login(credentials: LoginCredentials): Promise<UserDTO> {
    const { data } = await client.post<UserDTO>('/auth/login', credentials)
    return data
  },

  async logout(): Promise<void> {
    await client.post('/auth/logout')
  },

  async getMe(): Promise<UserDTO> {
    const { data } = await client.get<UserDTO>('/users/me')
    return data
  },
}