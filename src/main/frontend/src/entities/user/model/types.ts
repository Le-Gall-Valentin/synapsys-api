export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER'

export interface User {
  id: string
  username: string
  email: string
  role: UserRole
  createdAt: string
  totpEnabled: boolean
}

export function isAdminRole(role?: UserRole): boolean {
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
}