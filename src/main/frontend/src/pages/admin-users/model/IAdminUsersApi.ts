import type { AdminUser } from '@/entities/user'

/** Paginated users payload returned by the backend (Spring-style page). */
export interface UsersPage {
  content: AdminUser[]
  totalElements: number
  page: number
  size: number
}

/**
 * Port for admin user management. Consumers (hooks) depend on this contract,
 * never on the concrete axios-backed implementation, which is injected through
 * AdminUsersApiProvider.
 */
export interface IAdminUsersApi {
  listUsers(page: number, size?: number, search?: string): Promise<UsersPage>
  createUser(username: string, email: string, password: string, role: 'USER' | 'ADMIN'): Promise<void>
  updateUserRole(id: string, role: 'USER' | 'ADMIN'): Promise<void>
  activateUser(id: string): Promise<void>
  deactivateUser(id: string): Promise<void>
  resetTotp(id: string): Promise<void>
  deleteUser(id: string): Promise<void>
}
