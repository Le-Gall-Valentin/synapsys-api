import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { TokenStatusPill } from './TokenStatusPill'

describe('TokenStatusPill', () => {
  it('renders the provided label', () => {
    render(<TokenStatusPill status="ACTIVE" label="Actif" />)
    const element = screen.getByText('Actif')
    expect(element).toBeDefined()
  })

  it('applies a status-specific class for REVOKED', () => {
    render(<TokenStatusPill status="REVOKED" label="Révoqué" />)
    expect(screen.getByText('Révoqué').className).toContain('status-orange')
  })
})
