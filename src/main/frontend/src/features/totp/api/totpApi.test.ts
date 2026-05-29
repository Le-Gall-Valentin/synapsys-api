// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { totpApi } from './totpApi'
import { client } from '@/shared/api'
import { TotpCodeError, TotpChallengeExpiredError, TotpAlreadyEnabledError } from '../model/errors'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

vi.mock('@/shared/api', () => ({
  client: {
    post: vi.fn(),
  },
}))

const mockedClient = client as unknown as {
  post: ReturnType<typeof vi.fn>
}

function makeAxiosError(
  status: number,
  message = 'error',
  headers: Record<string, string> = {},
  data: Record<string, unknown> = {},
): AxiosError {
  return new axios.AxiosError(message, undefined, undefined, undefined, {
    status,
    data,
    headers,
    config: {} as never,
    statusText: String(status),
  })
}

describe('totpApi', () => {
  beforeEach(() => {
    mockedClient.post.mockReset()
  })

  // verify
  describe('verify', () => {
    it('returns User on success', async () => {
      const user = { id: '1', username: 'user', role: 'USER', totpEnabled: true }
      mockedClient.post.mockResolvedValue({ data: user })

      await expect(totpApi.verify('123456')).resolves.toEqual(user)
      expect(mockedClient.post).toHaveBeenCalledWith('/auth/2fa/verify', { code: '123456' })
    })

    it('throws TotpCodeError on 401 with non-challenge title', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(401, 'Unauthorized', {}, { title: 'InvalidCode' }))
      await expect(totpApi.verify('000000')).rejects.toBeInstanceOf(TotpCodeError)
    })

    it('throws TotpChallengeExpiredError on 401 with TotpChallengeExpired title', async () => {
      mockedClient.post.mockRejectedValue(
        makeAxiosError(401, 'Unauthorized', {}, { title: 'TotpChallengeExpired' }),
      )
      await expect(totpApi.verify('123456')).rejects.toBeInstanceOf(TotpChallengeExpiredError)
    })

    it('throws RateLimitError on 429', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429, 'Too Many Requests', { 'retry-after': '30' }))
      const caught = await totpApi.verify('123456').catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBe(30)
    })

    it('throws RateLimitError with null retryAfterSeconds when header absent', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429))
      const caught = await totpApi.verify('123456').catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBeNull()
    })

    it('throws ServerError on 500', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(500))
      await expect(totpApi.verify('123456')).rejects.toBeInstanceOf(ServerError)
    })

    it('throws NetworkError on no response', async () => {
      mockedClient.post.mockRejectedValue(new Error('Network Error'))
      await expect(totpApi.verify('123456')).rejects.toBeInstanceOf(NetworkError)
    })
  })

  // setup
  describe('setup', () => {
    it('returns TotpSetupData on success', async () => {
      const setupData = { otpauthUri: 'otpauth://totp/App:user?secret=ABC', secret: 'ABC' }
      mockedClient.post.mockResolvedValue({ data: setupData })

      await expect(totpApi.setup()).resolves.toEqual(setupData)
      expect(mockedClient.post).toHaveBeenCalledWith('/auth/2fa/setup')
    })

    it('throws TotpAlreadyEnabledError on 409', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(409))
      await expect(totpApi.setup()).rejects.toBeInstanceOf(TotpAlreadyEnabledError)
    })

    it('throws ServerError on 500', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(500))
      await expect(totpApi.setup()).rejects.toBeInstanceOf(ServerError)
    })

    it('throws NetworkError on no response', async () => {
      mockedClient.post.mockRejectedValue(new Error('Network Error'))
      await expect(totpApi.setup()).rejects.toBeInstanceOf(NetworkError)
    })
  })

  // confirm
  describe('confirm', () => {
    it('resolves void on 204', async () => {
      mockedClient.post.mockResolvedValue({ status: 204, data: undefined })

      await expect(totpApi.confirm('123456')).resolves.toBeUndefined()
      expect(mockedClient.post).toHaveBeenCalledWith('/auth/2fa/confirm', { code: '123456' })
    })

    it('throws TotpCodeError on 401', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(401))
      await expect(totpApi.confirm('000000')).rejects.toBeInstanceOf(TotpCodeError)
    })

    it('throws ServerError on 500', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(500))
      await expect(totpApi.confirm('123456')).rejects.toBeInstanceOf(ServerError)
    })

    it('throws NetworkError on no response', async () => {
      mockedClient.post.mockRejectedValue(new Error('Network Error'))
      await expect(totpApi.confirm('123456')).rejects.toBeInstanceOf(NetworkError)
    })
  })
})