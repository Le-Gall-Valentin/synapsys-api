import { describe, it, expect } from 'vitest'
import { formatExpiry } from './formatExpiry'

describe('formatExpiry', () => {
  it('returns the EXPIRED sentinel for an expired token', () => {
    expect(formatExpiry('2026-06-24T12:00:00Z', 'EXPIRED', 'fr')).toBe('__expired__')
  })

  it('formats a future date for an active token', () => {
    const out = formatExpiry('2026-06-24T12:00:00Z', 'ACTIVE', 'fr')
    expect(out).not.toBe('__expired__')
    expect(out).not.toBe('—')
  })

  it('returns an em dash for an invalid date', () => {
    expect(formatExpiry('nope', 'ACTIVE', 'fr')).toBe('—')
  })
})
