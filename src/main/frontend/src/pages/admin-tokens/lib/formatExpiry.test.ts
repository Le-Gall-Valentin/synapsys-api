import { describe, it, expect } from 'vitest'
import { formatExpiry } from './formatExpiry'

describe('formatExpiry', () => {
  it('formats an absolute localized date-time', () => {
    const out = formatExpiry('2026-06-24T12:00:00Z', 'fr')
    expect(out).not.toBe('—')
    expect(out.length).toBeGreaterThan(0)
  })

  it('localizes the date differently for fr and en', () => {
    expect(formatExpiry('2026-06-24T12:00:00Z', 'fr')).not.toBe(formatExpiry('2026-06-24T12:00:00Z', 'en'))
  })

  it('returns a dash for an invalid date', () => {
    expect(formatExpiry('nope', 'fr')).toBe('—')
  })
})
