import { describe, it, expect } from 'vitest'
import { mapApiErrorToKey } from './mapApiErrorToKey'
import { TokenNotRevocableError } from '../api/enrollmentTokensApi'
import { NetworkError, RateLimitError, ForbiddenError, NotFoundError, ServerError } from '@/shared/lib'

describe('mapApiErrorToKey', () => {
  it('maps TokenNotRevocableError to not_revocable', () => {
    expect(mapApiErrorToKey(new TokenNotRevocableError(), 'revoke')).toBe('revoke.error.not_revocable')
  })
  it('maps ForbiddenError to forbidden', () => {
    expect(mapApiErrorToKey(new ForbiddenError(), 'revoke')).toBe('revoke.error.forbidden')
  })
  it('maps NotFoundError to not_found', () => {
    expect(mapApiErrorToKey(new NotFoundError(), 'revoke')).toBe('revoke.error.not_found')
  })
  it('maps RateLimitError to rate_limit', () => {
    expect(mapApiErrorToKey(new RateLimitError(), 'create')).toBe('create.error.rate_limit')
  })
  it('maps NetworkError to network', () => {
    expect(mapApiErrorToKey(new NetworkError(), 'create')).toBe('create.error.network')
  })
  it('falls back to server for ServerError', () => {
    expect(mapApiErrorToKey(new ServerError(), 'create')).toBe('create.error.server')
  })
  it('falls back to server for unknown errors', () => {
    expect(mapApiErrorToKey(new Error('x'), 'create')).toBe('create.error.server')
  })
})
