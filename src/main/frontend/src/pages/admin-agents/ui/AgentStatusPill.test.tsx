import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { AgentStatusPill } from './AgentStatusPill'

describe('AgentStatusPill', () => {
  it('renders the provided label', () => {
    render(<AgentStatusPill status="ACTIVE" label="Actif" />)
    expect(screen.getByText('Actif')).toBeDefined()
  })

  it('applies the green class for ACTIVE', () => {
    render(<AgentStatusPill status="ACTIVE" label="Actif" />)
    expect(screen.getByText('Actif').className).toContain('status-green')
  })

  it('applies the red class for REVOKED', () => {
    render(<AgentStatusPill status="REVOKED" label="Révoqué" />)
    expect(screen.getByText('Révoqué').className).toContain('status-red')
  })
})