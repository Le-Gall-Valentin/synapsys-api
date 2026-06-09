import { describe, it, expect } from 'vitest'
import {
  canDeactivate, canActivate, canDelete, canResetTotp, canEditRole,
  type AdminUser,
} from './adminPermissions'
import type { User } from '../model/types'

function makeUser(overrides: Partial<User> = {}): User {
  return { id: 'u-1', username: 'alice', email: 'alice@test.com', role: 'USER', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false, ...overrides }
}

function makeAdminUser(overrides: Partial<AdminUser> = {}): AdminUser {
  return { ...makeUser(), isActive: true, ...overrides }
}

const superAdmin = makeUser({ id: 'sa', role: 'SUPER_ADMIN' })
const admin = makeUser({ id: 'a1', role: 'ADMIN' })
const user = makeUser({ id: 'u1', role: 'USER' })

const targetSuperAdmin = makeAdminUser({ id: 'sa-t', role: 'SUPER_ADMIN' })
const targetAdmin = makeAdminUser({ id: 'a-t', role: 'ADMIN' })
const targetUser = makeAdminUser({ id: 'u-t', role: 'USER' })
const targetUserTotpOn = makeAdminUser({ id: 'u-t2', role: 'USER', totpEnabled: true })

// ─── canDeactivate ───────────────────────────────────────────────

describe('canDeactivate', () => {
  it('SUPER_ADMIN can deactivate ADMIN', () => {
    expect(canDeactivate(superAdmin, targetAdmin).ok).toBe(true)
  })
  it('SUPER_ADMIN can deactivate USER', () => {
    expect(canDeactivate(superAdmin, targetUser).ok).toBe(true)
  })
  it('cannot deactivate SUPER_ADMIN', () => {
    expect(canDeactivate(superAdmin, targetSuperAdmin).ok).toBe(false)
  })
  it('ADMIN can deactivate USER', () => {
    expect(canDeactivate(admin, targetUser).ok).toBe(true)
  })
  it('ADMIN cannot deactivate another ADMIN', () => {
    expect(canDeactivate(admin, targetAdmin).ok).toBe(false)
  })
  it('USER cannot deactivate anyone', () => {
    expect(canDeactivate(user, targetUser).ok).toBe(false)
  })
  it('cannot deactivate self', () => {
    const self = makeAdminUser({ id: 'sa', role: 'SUPER_ADMIN' })
    expect(canDeactivate(superAdmin, self).ok).toBe(false)
  })
})

// ─── canActivate ─────────────────────────────────────────────────

describe('canActivate', () => {
  it('SUPER_ADMIN can activate ADMIN', () => {
    expect(canActivate(superAdmin, makeAdminUser({ id: 'x', role: 'ADMIN', isActive: false })).ok).toBe(true)
  })
  it('ADMIN can activate USER', () => {
    expect(canActivate(admin, makeAdminUser({ id: 'x', role: 'USER', isActive: false })).ok).toBe(true)
  })
  it('ADMIN cannot activate another ADMIN', () => {
    expect(canActivate(admin, makeAdminUser({ id: 'x', role: 'ADMIN', isActive: false })).ok).toBe(false)
  })
  it('USER cannot activate anyone', () => {
    expect(canActivate(user, makeAdminUser({ id: 'x', role: 'USER', isActive: false })).ok).toBe(false)
  })
  it('cannot activate self', () => {
    expect(canActivate(admin, makeAdminUser({ id: 'a1', role: 'ADMIN', isActive: false })).ok).toBe(false)
  })
})

// ─── canDelete ───────────────────────────────────────────────────

describe('canDelete', () => {
  it('SUPER_ADMIN can delete ADMIN', () => {
    expect(canDelete(superAdmin, targetAdmin).ok).toBe(true)
  })
  it('SUPER_ADMIN can delete USER', () => {
    expect(canDelete(superAdmin, targetUser).ok).toBe(true)
  })
  it('cannot delete SUPER_ADMIN', () => {
    expect(canDelete(superAdmin, targetSuperAdmin).ok).toBe(false)
  })
  it('ADMIN can delete USER', () => {
    expect(canDelete(admin, targetUser).ok).toBe(true)
  })
  it('ADMIN cannot delete another ADMIN', () => {
    expect(canDelete(admin, targetAdmin).ok).toBe(false)
  })
  it('USER cannot delete anyone', () => {
    expect(canDelete(user, targetUser).ok).toBe(false)
  })
  it('cannot delete self', () => {
    expect(canDelete(admin, makeAdminUser({ id: 'a1', role: 'ADMIN' })).ok).toBe(false)
  })
})

// ─── canResetTotp ────────────────────────────────────────────────

describe('canResetTotp', () => {
  it('SUPER_ADMIN can reset TOTP of ADMIN with totp enabled', () => {
    expect(canResetTotp(superAdmin, makeAdminUser({ id: 'x', role: 'ADMIN', totpEnabled: true })).ok).toBe(true)
  })
  it('SUPER_ADMIN can reset TOTP of USER with totp enabled', () => {
    expect(canResetTotp(superAdmin, targetUserTotpOn).ok).toBe(true)
  })
  it('ADMIN can reset TOTP of USER with totp enabled', () => {
    expect(canResetTotp(admin, targetUserTotpOn).ok).toBe(true)
  })
  it('ADMIN cannot reset TOTP of another ADMIN', () => {
    expect(canResetTotp(admin, makeAdminUser({ id: 'x', role: 'ADMIN', totpEnabled: true })).ok).toBe(false)
  })
  it('USER cannot reset TOTP of anyone', () => {
    expect(canResetTotp(user, targetUserTotpOn).ok).toBe(false)
  })
  it('cannot reset TOTP if not enabled', () => {
    expect(canResetTotp(superAdmin, targetUser).ok).toBe(false)
  })
  it('cannot reset own TOTP', () => {
    expect(canResetTotp(superAdmin, makeAdminUser({ id: 'sa', role: 'SUPER_ADMIN', totpEnabled: true })).ok).toBe(false)
  })
})

// ─── canEditRole ─────────────────────────────────────────────────

describe('canEditRole', () => {
  it('SUPER_ADMIN can edit ADMIN role', () => {
    expect(canEditRole(superAdmin, targetAdmin).ok).toBe(true)
  })
  it('SUPER_ADMIN can edit USER role', () => {
    expect(canEditRole(superAdmin, targetUser).ok).toBe(true)
  })
  it('cannot edit SUPER_ADMIN role', () => {
    expect(canEditRole(superAdmin, targetSuperAdmin).ok).toBe(false)
  })
  it('ADMIN can edit USER role', () => {
    expect(canEditRole(admin, targetUser).ok).toBe(true)
  })
  it('ADMIN cannot edit another ADMIN role', () => {
    expect(canEditRole(admin, targetAdmin).ok).toBe(false)
  })
  it('USER cannot edit anyone', () => {
    expect(canEditRole(user, targetUser).ok).toBe(false)
  })
  it('cannot edit own role', () => {
    expect(canEditRole(admin, makeAdminUser({ id: 'a1', role: 'ADMIN' })).ok).toBe(false)
  })
})
