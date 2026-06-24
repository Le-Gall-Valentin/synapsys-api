import { describe, it, expect, vi, beforeEach } from 'vitest'
import { agentsApi, AgentNotRevocableError } from './agentsApi'
import { client } from '@/shared/api'
import { ForbiddenError, NotFoundError, RateLimitError, ServerError, NetworkError } from '@/shared/lib'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn(), post: vi.fn(), delete: vi.fn() } }))

const mockClient = client as unknown as { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> }

function axiosError(status: number) {
  return { isAxiosError: true, response: { status } }
}

beforeEach(() => vi.clearAllMocks())

describe('agentsApi.listAgents', () => {
  it('passes pagination, sort and search params and returns the page', async () => {
    const page = { content: [], totalElements: 0, page: 0, size: 20 }
    mockClient.get.mockResolvedValue({ data: page })
    const result = await agentsApi.listAgents(1, 20, 'serverName', 'asc', 'web')
    expect(mockClient.get).toHaveBeenCalledWith('/agents', {
      params: { page: 1, size: 20, sortBy: 'serverName', sortDirection: 'asc', search: 'web' },
    })
    expect(result).toBe(page)
  })

  it('omits search when not provided', async () => {
    mockClient.get.mockResolvedValue({ data: { content: [], totalElements: 0, page: 0, size: 20 } })
    await agentsApi.listAgents(0, 20, 'enrolledAt', 'desc')
    expect(mockClient.get).toHaveBeenCalledWith('/agents', {
      params: { page: 0, size: 20, sortBy: 'enrolledAt', sortDirection: 'desc' },
    })
  })

  it('maps 403 to ForbiddenError', async () => {
    mockClient.get.mockRejectedValue(axiosError(403))
    await expect(agentsApi.listAgents(0, 20, 'enrolledAt', 'desc')).rejects.toBeInstanceOf(ForbiddenError)
  })

  it('maps 429 to RateLimitError', async () => {
    mockClient.get.mockRejectedValue(axiosError(429))
    await expect(agentsApi.listAgents(0, 20, 'enrolledAt', 'desc')).rejects.toBeInstanceOf(RateLimitError)
  })

  it('maps a network failure to NetworkError', async () => {
    mockClient.get.mockRejectedValue(new Error('boom'))
    await expect(agentsApi.listAgents(0, 20, 'enrolledAt', 'desc')).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('agentsApi.getStatistics', () => {
  it('returns the statistics payload', async () => {
    const stats = { active: 1, inactive: 0, pending: 2, revoked: 1, total: 4 }
    mockClient.get.mockResolvedValue({ data: stats })
    expect(await agentsApi.getStatistics()).toBe(stats)
  })

  it('maps 403 to ForbiddenError', async () => {
    mockClient.get.mockRejectedValue(axiosError(403))
    await expect(agentsApi.getStatistics()).rejects.toBeInstanceOf(ForbiddenError)
  })

  it('maps a network failure to NetworkError', async () => {
    mockClient.get.mockRejectedValue(new Error('boom'))
    await expect(agentsApi.getStatistics()).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('agentsApi.revokeAgent', () => {
  it('maps 409 to AgentNotRevocableError', async () => {
    mockClient.post.mockRejectedValue(axiosError(409))
    await expect(agentsApi.revokeAgent('a-1')).rejects.toBeInstanceOf(AgentNotRevocableError)
  })

  it('maps 404 to NotFoundError', async () => {
    mockClient.post.mockRejectedValue(axiosError(404))
    await expect(agentsApi.revokeAgent('a-1')).rejects.toBeInstanceOf(NotFoundError)
  })
})

describe('agentsApi.deleteAgent', () => {
  it('maps 409 to AgentNotRevocableError', async () => {
    mockClient.delete.mockRejectedValue(axiosError(409))
    await expect(agentsApi.deleteAgent('a-1')).rejects.toBeInstanceOf(AgentNotRevocableError)
  })

  it('maps 500 to ServerError', async () => {
    mockClient.delete.mockRejectedValue(axiosError(500))
    await expect(agentsApi.deleteAgent('a-1')).rejects.toBeInstanceOf(ServerError)
  })
})