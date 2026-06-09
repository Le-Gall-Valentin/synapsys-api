import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import type { AdminUser } from '@/entities/user'

export type { AdminUser }

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
  async listUsers(): Promise<AdminUser[]> {
    try {
      const res = await client.get<{ content: AdminUser[] }>('/users', {
        params: { page: 0, size: 500 },
      })
      return res.data.content
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
