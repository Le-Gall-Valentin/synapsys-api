import type { EnrollmentTokenStatus } from '../model/IEnrollmentTokensApi'

/**
 * Expiry column. Expired tokens return the '__expired__' sentinel so the caller
 * can render a localized label; otherwise an absolute localized date-time.
 */
export function formatExpiry(iso: string, status: EnrollmentTokenStatus, lang: string): string {
  if (status === 'EXPIRED') return '__expired__'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(lang === 'fr' ? 'fr-FR' : 'en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}
