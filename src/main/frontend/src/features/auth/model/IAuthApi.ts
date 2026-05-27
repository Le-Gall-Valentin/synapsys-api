import type { User } from '@/entities/user'
import type { LoginCredentials } from './types'

export interface IAuthApi {
  login(credentials: LoginCredentials): Promise<User>
  logout(): Promise<void>
  getMe(): Promise<User>
}