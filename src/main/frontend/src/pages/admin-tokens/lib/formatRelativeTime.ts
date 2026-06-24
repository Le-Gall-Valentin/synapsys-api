const DIVISIONS: { amount: number; unit: Intl.RelativeTimeFormatUnit }[] = [
  { amount: 60, unit: 'second' },
  { amount: 60, unit: 'minute' },
  { amount: 24, unit: 'hour' },
  { amount: 7, unit: 'day' },
  { amount: 4.34524, unit: 'week' },
  { amount: 12, unit: 'month' },
  { amount: Number.POSITIVE_INFINITY, unit: 'year' },
]

/** Relative "time ago" for the created-at column, localized via Intl.RelativeTimeFormat. */
export function formatRelativeTime(iso: string, lang: string, now: Date = new Date()): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  const locale = lang === 'fr' ? 'fr-FR' : 'en-GB'
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })
  let duration = (date.getTime() - now.getTime()) / 1000
  for (const division of DIVISIONS) {
    if (Math.abs(duration) < division.amount) {
      return rtf.format(Math.round(duration), division.unit)
    }
    duration /= division.amount
  }
  return '—'
}
