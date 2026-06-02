export function parseRetryAfter(headers: Record<string, string> | undefined): number | null {
  const value = headers?.['retry-after']
  if (!value) return null
  const parsed = parseInt(value, 10)
  return Number.isFinite(parsed) ? parsed : null
}