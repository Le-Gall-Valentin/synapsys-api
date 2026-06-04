import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, ServerError } from '@/shared/lib'

export class ConflictError extends Error {
  constructor() { super('Username or email already taken'); this.name = 'ConflictError' }
}

export class InvalidCurrentPasswordError extends Error {
  constructor() { super('Invalid current password'); this.name = 'InvalidCurrentPasswordError' }
}

export const accountApi = {
  async updateProfile(username: string, email: string): Promise<void> {
    try {
      await client.patch('/users/me', { username, email })
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 409) throw new ConflictError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    try {
      await client.patch('/users/me/password', { currentPassword, newPassword })
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 422) throw new InvalidCurrentPasswordError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },
}