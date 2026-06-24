import { describe, it, expect } from 'vitest'
import { formatAgentDate } from './formatAgentDate'

describe('formatAgentDate', () => {
  it('formats a valid ISO date', () => {
    expect(formatAgentDate('2026-02-10T09:14:00Z', 'fr')).not.toBe('—')
  })

  it('returns a dash for an invalid date', () => {
    expect(formatAgentDate('not-a-date', 'fr')).toBe('—')
  })
})