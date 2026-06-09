// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { adminUsersApi, ConflictError, RoleAlreadyAssignedError } from './adminUsersApi'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}))

const mock = client as unknown as Record<string, ReturnType<typeof vi.fn>>

function axiosErr(status: number): AxiosError {
  return new axios.AxiosError('err', undefined, undefined, undefined, {
    status, data: {}, headers: {}, config: {} as never, statusText: String(status),
  })
}

const ADMIN_USER = {
  id: 'u-1', username: 'alice', email: 'alice@test.com',
  role: 'ADMIN', isActive: true, createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

beforeEach(() => { Object.values(mock).forEach(m => m.mockReset()) })

describe('listUsers', () => {
  it('GET /users?page=0&size=500 and returns content array', async () => {
    mock.get.mockResolvedValue({ data: { content: [ADMIN_USER] } })
    const result = await adminUsersApi.listUsers()
    expect(mock.get).toHaveBeenCalledWith('/users', { params: { page: 0, size: 500 } })
    expect(result).toEqual([ADMIN_USER])
  })

  it('throws ServerError on 500', async () => {
    mock.get.mockRejectedValue(axiosErr(500))
    await expect(adminUsersApi.listUsers()).rejects.toBeInstanceOf(ServerError)
  })

  it('throws RateLimitError on 429', async () => {
    mock.get.mockRejectedValue(axiosErr(429))
    await expect(adminUsersApi.listUsers()).rejects.toBeInstanceOf(RateLimitError)
  })

  it('throws NetworkError on no response', async () => {
    mock.get.mockRejectedValue(new Error('network'))
    await expect(adminUsersApi.listUsers()).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('createUser', () => {
  it('POST /users with correct body', async () => {
    mock.post.mockResolvedValue({ status: 201 })
    await adminUsersApi.createUser('bob', 'bob@test.com', 'P@ss1!', 'USER')
    expect(mock.post).toHaveBeenCalledWith('/users', {
      username: 'bob', email: 'bob@test.com', password: 'P@ss1!', role: 'USER',
    })
  })

  it('throws ConflictError on 409', async () => {
    mock.post.mockRejectedValue(axiosErr(409))
    await expect(adminUsersApi.createUser('bob', 'bob@test.com', 'P@ss1!', 'USER')).rejects.toBeInstanceOf(ConflictError)
  })

  it('throws RateLimitError on 429', async () => {
    mock.post.mockRejectedValue(axiosErr(429))
    await expect(adminUsersApi.createUser('bob', 'bob@test.com', 'P@ss1!', 'USER')).rejects.toBeInstanceOf(RateLimitError)
  })

  it('throws ServerError on 500', async () => {
    mock.post.mockRejectedValue(axiosErr(500))
    await expect(adminUsersApi.createUser('bob', 'bob@test.com', 'P@ss1!', 'USER')).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.post.mockRejectedValue(new Error('network'))
    await expect(adminUsersApi.createUser('bob', 'bob@test.com', 'P@ss1!', 'USER')).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('updateUserRole', () => {
  it('PATCH /users/{id} with role', async () => {
    mock.patch.mockResolvedValue({ status: 204 })
    await adminUsersApi.updateUserRole('u-1', 'ADMIN')
    expect(mock.patch).toHaveBeenCalledWith('/users/u-1', { role: 'ADMIN' })
  })

  it('throws RoleAlreadyAssignedError on 409', async () => {
    mock.patch.mockRejectedValue(axiosErr(409))
    await expect(adminUsersApi.updateUserRole('u-1', 'ADMIN')).rejects.toBeInstanceOf(RoleAlreadyAssignedError)
  })

  it('throws RateLimitError on 429', async () => {
    mock.patch.mockRejectedValue(axiosErr(429))
    await expect(adminUsersApi.updateUserRole('u-1', 'ADMIN')).rejects.toBeInstanceOf(RateLimitError)
  })

  it('throws ServerError on 500', async () => {
    mock.patch.mockRejectedValue(axiosErr(500))
    await expect(adminUsersApi.updateUserRole('u-1', 'ADMIN')).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.patch.mockRejectedValue(new Error('network'))
    await expect(adminUsersApi.updateUserRole('u-1', 'ADMIN')).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('activateUser', () => {
  it('POST /users/{id}/activate', async () => {
    mock.post.mockResolvedValue({ status: 204 })
    await adminUsersApi.activateUser('u-1')
    expect(mock.post).toHaveBeenCalledWith('/users/u-1/activate')
  })

  it('throws ServerError on 500', async () => {
    mock.post.mockRejectedValue(axiosErr(500))
    await expect(adminUsersApi.activateUser('u-1')).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.post.mockRejectedValue(new Error('network'))
    await expect(adminUsersApi.activateUser('u-1')).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('deactivateUser', () => {
  it('POST /users/{id}/deactivate', async () => {
    mock.post.mockResolvedValue({ status: 204 })
    await adminUsersApi.deactivateUser('u-1')
    expect(mock.post).toHaveBeenCalledWith('/users/u-1/deactivate')
  })

  it('throws ServerError on 500', async () => {
    mock.post.mockRejectedValue(axiosErr(500))
    await expect(adminUsersApi.deactivateUser('u-1')).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.post.mockRejectedValue(new Error('network'))
    await expect(adminUsersApi.deactivateUser('u-1')).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('resetTotp', () => {
  it('POST /users/{id}/totp/reset', async () => {
    mock.post.mockResolvedValue({ status: 204 })
    await adminUsersApi.resetTotp('u-1')
    expect(mock.post).toHaveBeenCalledWith('/users/u-1/totp/reset')
  })

  it('throws ServerError on 500', async () => {
    mock.post.mockRejectedValue(axiosErr(500))
    await expect(adminUsersApi.resetTotp('u-1')).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.post.mockRejectedValue(new Error('network'))
    await expect(adminUsersApi.resetTotp('u-1')).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('deleteUser', () => {
  it('DELETE /users/{id}', async () => {
    mock.delete.mockResolvedValue({ status: 204 })
    await adminUsersApi.deleteUser('u-1')
    expect(mock.delete).toHaveBeenCalledWith('/users/u-1')
  })

  it('throws ServerError on 500', async () => {
    mock.delete.mockRejectedValue(axiosErr(500))
    await expect(adminUsersApi.deleteUser('u-1')).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.delete.mockRejectedValue(new Error('network'))
    await expect(adminUsersApi.deleteUser('u-1')).rejects.toBeInstanceOf(NetworkError)
  })
})
