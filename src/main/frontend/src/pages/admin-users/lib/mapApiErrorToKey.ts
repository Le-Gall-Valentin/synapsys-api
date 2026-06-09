import { NetworkError, RateLimitError } from '@/shared/lib'
import { ConflictError, RoleAlreadyAssignedError } from '../api/adminUsersApi'

/**
 * Maps an API error to a translation key under the given namespace prefix.
 * All admin-users error namespaces share the same suffixes
 * (conflict, already_assigned, rate_limit, network, server).
 */
export function mapApiErrorToKey(error: unknown, prefix: string): string {
  if (error instanceof ConflictError) return `${prefix}.error.conflict`
  if (error instanceof RoleAlreadyAssignedError) return `${prefix}.error.already_assigned`
  if (error instanceof RateLimitError) return `${prefix}.error.rate_limit`
  if (error instanceof NetworkError) return `${prefix}.error.network`
  return `${prefix}.error.server`
}
