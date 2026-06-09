import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import type { AdminUser } from '@/entities/user'

export type { AdminUser }

export interface UsersPage {
  content: AdminUser[]
  totalElements: number
  page: number
  size: number
}

export class ConflictError extends Error {
  constructor() { super('Username or email already taken'); this.name = 'ConflictError' }
}

export class RoleAlreadyAssignedError extends Error {
  constructor() { super('User already has this role'); this.name = 'RoleAlreadyAssignedError' }
}

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const adminUsersApi = {
  async listUsers(page: number, size = 20): Promise<UsersPage> {
    try {
      const res = await client.get<UsersPage>('/users', { params: { page, size } })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async createUser(
    username: string,
    email: string,
    password: string,
    role: 'USER' | 'ADMIN',
  ): Promise<void> {
    try {
      await client.post('/users', { username, email, password, role })
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 409) throw new ConflictError()
        if (status === 429) throw new RateLimitError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async updateUserRole(id: string, role: 'USER' | 'ADMIN'): Promise<void> {
    try {
      await client.patch(`/users/${id}`, { role })
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 409) throw new RoleAlreadyAssignedError()
        if (status === 429) throw new RateLimitError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async activateUser(id: string): Promise<void> {
    try {
      await client.post(`/users/${id}/activate`)
    } catch (error) {
      handleError(error)
    }
  },

  async deactivateUser(id: string): Promise<void> {
    try {
      await client.post(`/users/${id}/deactivate`)
    } catch (error) {
      handleError(error)
    }
  },

  async resetTotp(id: string): Promise<void> {
    try {
      await client.post(`/users/${id}/totp/reset`)
    } catch (error) {
      handleError(error)
    }
  },

  async deleteUser(id: string): Promise<void> {
    try {
      await client.delete(`/users/${id}`)
    } catch (error) {
      handleError(error)
    }
  },
}
