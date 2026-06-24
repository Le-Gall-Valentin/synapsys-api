import { NetworkError, RateLimitError, ForbiddenError, NotFoundError } from '@/shared/lib'
import { AgentNotRevocableError } from '../api/agentsApi'

/**
 * Mappe une erreur API vers une clé de traduction sous le préfixe donné (revoke | delete).
 * Suffixes : conflict, forbidden, not_found, rate_limit, network, server.
 */
export function mapApiErrorToKey(error: unknown, prefix: string): string {
  if (error instanceof AgentNotRevocableError) return `${prefix}.error.conflict`
  if (error instanceof ForbiddenError) return `${prefix}.error.forbidden`
  if (error instanceof NotFoundError) return `${prefix}.error.not_found`
  if (error instanceof RateLimitError) return `${prefix}.error.rate_limit`
  if (error instanceof NetworkError) return `${prefix}.error.network`
  return `${prefix}.error.server`
}