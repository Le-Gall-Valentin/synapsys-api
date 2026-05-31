import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import type { User } from '@/entities/user'
import type { ITotpApi } from './ITotpApi'
import type { TotpSetupData } from '../model/types'
import { TotpAlreadyEnabledError, TotpChallengeExpiredError, TotpCodeError, TotpMaxAttemptsError } from '../model/errors'
import { NetworkError, ServerError, RateLimitError } from '@/shared/lib'

export const totpApi: ITotpApi = {
  async verify(code: string): Promise<User> {
    try {
      const { data } = await client.post<User>('/auth/2fa/verify', { code })
      return data
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) {
          // Backend ProblemDetail title distinguishes challenge expiry from wrong code.
          // 'TotpChallengeExpired' is set in AuthExceptionHandler for TotpChallengeExpired exception.
          // Any other 401 title (or absent title) maps to a wrong code.
          // Backend title distinguishes the three 401 cases:
          // 'TotpChallengeExpired'     → session timeout
          // 'TotpMaxAttemptsExceeded'  → brute-force lockout after 5 failures
          // anything else / absent     → wrong code
          const title = error.response?.data?.title as string | undefined
          if (title === 'TotpChallengeExpired') throw new TotpChallengeExpiredError()
          if (title === 'TotpMaxAttemptsExceeded') throw new TotpMaxAttemptsError()
          throw new TotpCodeError()
        }
        if (status === 429) {
          const retryAfter = error.response?.headers?.['retry-after']
          const parsed = retryAfter ? parseInt(retryAfter, 10) : NaN
          const seconds = Number.isFinite(parsed) ? parsed : null
          throw new RateLimitError(seconds)
        }
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async setup(): Promise<TotpSetupData> {
    try {
      const { data } = await client.post<TotpSetupData>('/auth/2fa/setup')
      return data
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 409) throw new TotpAlreadyEnabledError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async confirm(code: string): Promise<void> {
    try {
      await client.post('/auth/2fa/confirm', { code })
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) throw new TotpCodeError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async getStatus(): Promise<{ totpEnabled: boolean }> {
    try {
      const { data } = await client.get<{ totpEnabled: boolean }>('/auth/2fa/status')
      return data
    } catch (error) {
      if (isAxiosError(error)) {
        if (error.response?.status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },
}