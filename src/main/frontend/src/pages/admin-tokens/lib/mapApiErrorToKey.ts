import { NetworkError, RateLimitError, ForbiddenError, NotFoundError } from '@/shared/lib'
import { TokenNotRevocableError } from '../api/enrollmentTokensApi'

/**
 * Maps an API error to a translation key under the given namespace prefix
 * (create | revoke). Suffixes: not_revocable, forbidden, not_found, rate_limit,
 * network, server.
 */
export function mapApiErrorToKey(error: unknown, prefix: string): string {
  if (error instanceof TokenNotRevocableError) return `${prefix}.error.not_revocable`
  if (error instanceof ForbiddenError) return `${prefix}.error.forbidden`
  if (error instanceof NotFoundError) return `${prefix}.error.not_found`
  if (error instanceof RateLimitError) return `${prefix}.error.rate_limit`
  if (error instanceof NetworkError) return `${prefix}.error.network`
  return `${prefix}.error.server`
}
