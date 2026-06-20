import { describe, it, expect } from 'vitest'
import { formatUserDate } from './formatUserDate'

// Midday UTC keeps the calendar day stable across local timezones.
const DATE = '2024-02-15T12:00:00Z'

describe('formatUserDate', () => {
  it('includes the day and year', () => {
    const formatted = formatUserDate(DATE, 'fr')
    expect(formatted).toContain('2024')
    expect(formatted).toContain('15')
  })

  it('localizes the month differently for fr and en', () => {
    expect(formatUserDate(DATE, 'fr')).not.toBe(formatUserDate(DATE, 'en'))
  })
})
