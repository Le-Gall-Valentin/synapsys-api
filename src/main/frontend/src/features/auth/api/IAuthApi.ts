import type { UserDTO } from '@/entities/user'
import type { LoginCredentials } from '../model/types'

export interface IAuthApi {
  login(credentials: LoginCredentials): Promise<UserDTO>
  logout(): Promise<void>
  getMe(): Promise<UserDTO>
}