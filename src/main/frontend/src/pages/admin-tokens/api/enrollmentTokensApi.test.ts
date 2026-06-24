// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { enrollmentTokensApi, TokenNotRevocableError } from './enrollmentTokensApi'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn() },
}))

const mock = client as unknown as Record<string, ReturnType<typeof vi.fn>>

function axiosErr(status: number): AxiosError {
  return new axios.AxiosError('err', undefined, undefined, undefined, {
    status, data: {}, headers: {}, config: {} as never, statusText: String(status),
  })
}

const TOKEN = {
  id: 't-1', serverName: 'web-01', status: 'ACTIVE',
  expiresAt: '2026-06-24T12:00:00Z',
  createdBy: { id: 'u-1', username: 'alice' },
  createdAt: '2026-06-24T11:45:00Z',
}
const TOKENS_PAGE = { content: [TOKEN], totalElements: 1, page: 0, size: 20 }
const CREATED = { ...TOKEN, token: 'syn_enr_abc123_def456' }

beforeEach(() => { Object.values(mock).forEach(m => m.mockReset()) })

describe('listTokens', () => {
  it('GET /agents/enrollment-tokens?page=0&size=20 and returns TokensPage', async () => {
    mock.get.mockResolvedValue({ data: TOKENS_PAGE })
    const result = await enrollmentTokensApi.listTokens(0)
    expect(mock.get).toHaveBeenCalledWith('/agents/enrollment-tokens', { params: { page: 0, size: 20 } })
    expect(result).toEqual(TOKENS_PAGE)
  })

  it('passes the requested page', async () => {
    mock.get.mockResolvedValue({ data: TOKENS_PAGE })
    await enrollmentTokensApi.listTokens(3)
    expect(mock.get).toHaveBeenCalledWith('/agents/enrollment-tokens', { params: { page: 3, size: 20 } })
  })

  it('throws RateLimitError on 429', async () => {
    mock.get.mockRejectedValue(axiosErr(429))
    await expect(enrollmentTokensApi.listTokens(0)).rejects.toBeInstanceOf(RateLimitError)
  })

  it('throws ForbiddenError on 403', async () => {
    mock.get.mockRejectedValue(axiosErr(403))
    await expect(enrollmentTokensApi.listTokens(0)).rejects.toBeInstanceOf(ForbiddenError)
  })

  it('throws ServerError on 500', async () => {
    mock.get.mockRejectedValue(axiosErr(500))
    await expect(enrollmentTokensApi.listTokens(0)).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.get.mockRejectedValue(new Error('network'))
    await expect(enrollmentTokensApi.listTokens(0)).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('createToken', () => {
  it('POST /agents/enrollment-tokens with serverName + ttlMinutes', async () => {
    mock.post.mockResolvedValue({ data: CREATED })
    const result = await enrollmentTokensApi.createToken('web-01', 15)
    expect(mock.post).toHaveBeenCalledWith('/agents/enrollment-tokens', { serverName: 'web-01', ttlMinutes: 15 })
    expect(result).toEqual(CREATED)
  })

  it('omits ttlMinutes when undefined', async () => {
    mock.post.mockResolvedValue({ data: CREATED })
    await enrollmentTokensApi.createToken('web-01')
    expect(mock.post).toHaveBeenCalledWith('/agents/enrollment-tokens', { serverName: 'web-01' })
  })

  it('throws ForbiddenError on 403', async () => {
    mock.post.mockRejectedValue(axiosErr(403))
    await expect(enrollmentTokensApi.createToken('web-01', 15)).rejects.toBeInstanceOf(ForbiddenError)
  })

  it('throws RateLimitError on 429', async () => {
    mock.post.mockRejectedValue(axiosErr(429))
    await expect(enrollmentTokensApi.createToken('web-01', 15)).rejects.toBeInstanceOf(RateLimitError)
  })

  it('throws ServerError on 400', async () => {
    mock.post.mockRejectedValue(axiosErr(400))
    await expect(enrollmentTokensApi.createToken('web-01', 15)).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.post.mockRejectedValue(new Error('network'))
    await expect(enrollmentTokensApi.createToken('web-01', 15)).rejects.toBeInstanceOf(NetworkError)
  })
})

describe('revokeToken', () => {
  it('POST /agents/enrollment-tokens/{id}/revoke', async () => {
    mock.post.mockResolvedValue({ status: 204 })
    await enrollmentTokensApi.revokeToken('t-1')
    expect(mock.post).toHaveBeenCalledWith('/agents/enrollment-tokens/t-1/revoke')
  })

  it('throws TokenNotRevocableError on 409', async () => {
    mock.post.mockRejectedValue(axiosErr(409))
    await expect(enrollmentTokensApi.revokeToken('t-1')).rejects.toBeInstanceOf(TokenNotRevocableError)
  })

  it('throws NotFoundError on 404', async () => {
    mock.post.mockRejectedValue(axiosErr(404))
    await expect(enrollmentTokensApi.revokeToken('t-1')).rejects.toBeInstanceOf(NotFoundError)
  })

  it('throws ForbiddenError on 403', async () => {
    mock.post.mockRejectedValue(axiosErr(403))
    await expect(enrollmentTokensApi.revokeToken('t-1')).rejects.toBeInstanceOf(ForbiddenError)
  })

  it('throws ServerError on 500', async () => {
    mock.post.mockRejectedValue(axiosErr(500))
    await expect(enrollmentTokensApi.revokeToken('t-1')).rejects.toBeInstanceOf(ServerError)
  })

  it('throws NetworkError on no response', async () => {
    mock.post.mockRejectedValue(new Error('network'))
    await expect(enrollmentTokensApi.revokeToken('t-1')).rejects.toBeInstanceOf(NetworkError)
  })
})
