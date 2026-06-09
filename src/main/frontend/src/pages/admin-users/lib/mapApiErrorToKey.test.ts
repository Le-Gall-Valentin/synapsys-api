import { describe, it, expect } from 'vitest'
import { mapApiErrorToKey } from './mapApiErrorToKey'
import { ConflictError, RoleAlreadyAssignedError } from '../api/adminUsersApi'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

describe('mapApiErrorToKey', () => {
  it('maps ConflictError', () => {
    expect(mapApiErrorToKey(new ConflictError(), 'create')).toBe('create.error.conflict')
  })

  it('maps RoleAlreadyAssignedError', () => {
    expect(mapApiErrorToKey(new RoleAlreadyAssignedError(), 'edit_role')).toBe('edit_role.error.already_assigned')
  })

  it('maps RateLimitError', () => {
    expect(mapApiErrorToKey(new RateLimitError(), 'delete')).toBe('delete.error.rate_limit')
  })

  it('maps NetworkError', () => {
    expect(mapApiErrorToKey(new NetworkError(), 'reset_totp')).toBe('reset_totp.error.network')
  })

  it('maps ServerError and unknown errors to server', () => {
    expect(mapApiErrorToKey(new ServerError(), 'create')).toBe('create.error.server')
    expect(mapApiErrorToKey(new Error('boom'), 'create')).toBe('create.error.server')
  })
})
