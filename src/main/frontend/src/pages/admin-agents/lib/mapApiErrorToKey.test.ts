import { describe, it, expect } from 'vitest'
import { mapApiErrorToKey } from './mapApiErrorToKey'
import { AgentNotRevocableError } from '../api/agentsApi'
import { ForbiddenError, NotFoundError, RateLimitError, NetworkError } from '@/shared/lib'

describe('mapApiErrorToKey', () => {
  it('maps AgentNotRevocableError to the conflict key', () => {
    expect(mapApiErrorToKey(new AgentNotRevocableError(), 'revoke')).toBe('revoke.error.conflict')
  })
  it('maps ForbiddenError', () => {
    expect(mapApiErrorToKey(new ForbiddenError(), 'delete')).toBe('delete.error.forbidden')
  })
  it('maps NotFoundError', () => {
    expect(mapApiErrorToKey(new NotFoundError(), 'revoke')).toBe('revoke.error.not_found')
  })
  it('maps RateLimitError', () => {
    expect(mapApiErrorToKey(new RateLimitError(), 'revoke')).toBe('revoke.error.rate_limit')
  })
  it('maps NetworkError', () => {
    expect(mapApiErrorToKey(new NetworkError(), 'revoke')).toBe('revoke.error.network')
  })
  it('falls back to server for unknown errors', () => {
    expect(mapApiErrorToKey(new Error('x'), 'delete')).toBe('delete.error.server')
  })
})