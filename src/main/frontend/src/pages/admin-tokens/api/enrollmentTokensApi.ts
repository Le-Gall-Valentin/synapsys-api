import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { IEnrollmentTokensApi, TokensPage, CreatedToken } from '../model/IEnrollmentTokensApi'

const BASE = '/agents/enrollment-tokens'

/** Thrown when the backend refuses a revoke (409): token already consumed, expired or revoked. */
export class TokenNotRevocableError extends Error {
  constructor() { super('Token is no longer revocable'); this.name = 'TokenNotRevocableError' }
}

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    if (status === 404) throw new NotFoundError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const enrollmentTokensApi: IEnrollmentTokensApi = {
  async listTokens(page: number, size = 20): Promise<TokensPage> {
    try {
      const res = await client.get<TokensPage>(BASE, { params: { page, size } })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async createToken(serverName: string, ttlMinutes?: number): Promise<CreatedToken> {
    try {
      const body: { serverName: string; ttlMinutes?: number } = { serverName }
      if (ttlMinutes !== undefined) body.ttlMinutes = ttlMinutes
      const res = await client.post<CreatedToken>(BASE, body)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async revokeToken(id: string): Promise<void> {
    try {
      await client.post(`${BASE}/${id}/revoke`)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) throw new TokenNotRevocableError()
      handleError(error)
    }
  },
}
