export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER'

export interface User {
  id: string
  username: string
  role: UserRole
}

export function isAdminRole(role?: UserRole): boolean {
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
}