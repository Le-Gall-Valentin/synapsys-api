import type { User } from '../model/types'

export interface AdminUser extends User {
  isActive: boolean
}

interface PermissionResult {
  ok: boolean
  reason?: string
}

export function canDeactivate(caller: User, target: AdminUser): PermissionResult {
  if (target.role === 'SUPER_ADMIN') return { ok: false, reason: 'perm.super_admin_protected' }
  if (caller.id === target.id) return { ok: false, reason: 'perm.self' }
  if (caller.role === 'ADMIN' && target.role === 'ADMIN') return { ok: false, reason: 'perm.admin_cannot_manage_admin' }
  if (caller.role === 'USER') return { ok: false, reason: 'perm.insufficient' }
  return { ok: true }
}

export function canActivate(caller: User, target: AdminUser): PermissionResult {
  if (caller.id === target.id) return { ok: false, reason: 'perm.self' }
  if (caller.role === 'ADMIN' && target.role === 'ADMIN') return { ok: false, reason: 'perm.admin_cannot_manage_admin' }
  if (caller.role === 'USER') return { ok: false, reason: 'perm.insufficient' }
  return { ok: true }
}

export function canDelete(caller: User, target: AdminUser): PermissionResult {
  if (target.role === 'SUPER_ADMIN') return { ok: false, reason: 'perm.super_admin_protected' }
  if (caller.id === target.id) return { ok: false, reason: 'perm.self' }
  if (caller.role === 'ADMIN' && target.role === 'ADMIN') return { ok: false, reason: 'perm.admin_cannot_manage_admin' }
  if (caller.role === 'USER') return { ok: false, reason: 'perm.insufficient' }
  return { ok: true }
}

export function canResetTotp(caller: User, target: AdminUser): PermissionResult {
  if (caller.id === target.id) return { ok: false, reason: 'perm.self' }
  if (!target.totpEnabled) return { ok: false, reason: 'perm.totp_not_enabled' }
  if (caller.role === 'USER') return { ok: false, reason: 'perm.insufficient' }
  if (caller.role === 'ADMIN' && target.role !== 'USER') return { ok: false, reason: 'perm.admin_cannot_manage_admin' }
  return { ok: true }
}

export function canEditRole(caller: User, target: AdminUser): PermissionResult {
  if (target.role === 'SUPER_ADMIN') return { ok: false, reason: 'perm.super_admin_protected' }
  if (caller.id === target.id) return { ok: false, reason: 'perm.self' }
  if (caller.role === 'ADMIN' && target.role !== 'USER') return { ok: false, reason: 'perm.admin_cannot_manage_admin' }
  if (caller.role === 'USER') return { ok: false, reason: 'perm.insufficient' }
  return { ok: true }
}
