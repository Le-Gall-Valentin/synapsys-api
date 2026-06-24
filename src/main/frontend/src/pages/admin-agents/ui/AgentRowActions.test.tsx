import { fireEvent, render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AgentRowActions } from './AgentRowActions'
import type { Agent, AgentStatus } from '../model/IAgentsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k, i18n: { language: 'fr' } }),
}))

function agent(status: AgentStatus): Agent {
  return { id: 'a-1', serverName: 'srv', ipAddress: null, status, fingerprint: null, enrolledAt: '2026-02-10T09:14:00Z', lastActivityAt: null }
}

function setup(status: AgentStatus, extra = {}) {
  const onRevoke = vi.fn(); const onDelete = vi.fn()
  render(<AgentRowActions agent={agent(status)} onRevoke={onRevoke} onDelete={onDelete} {...extra} />)
  return { onRevoke, onDelete }
}

describe('AgentRowActions', () => {
  it('shows only revoke for ACTIVE', () => {
    setup('ACTIVE')
    expect(screen.getByText('table.revoke')).toBeDefined()
    expect(screen.queryByText('table.delete')).toBeNull()
  })

  it('shows only revoke for INACTIVE', () => {
    setup('INACTIVE')
    expect(screen.getByText('table.revoke')).toBeDefined()
    expect(screen.queryByText('table.delete')).toBeNull()
  })

  it('shows only delete for REVOKED', () => {
    setup('REVOKED')
    expect(screen.getByText('table.delete')).toBeDefined()
    expect(screen.queryByText('table.revoke')).toBeNull()
  })

  it('renders nothing for PENDING', () => {
    const { container } = render(<AgentRowActions agent={agent('PENDING')} onRevoke={vi.fn()} onDelete={vi.fn()} />)
    expect(container.firstChild).toBeNull()
  })

  it('disables the button while its own mutation is in flight', () => {
    setup('ACTIVE', { pendingActionId: 'a-1' })
    expect((screen.getByText('table.revoke').closest('button') as HTMLButtonElement).disabled).toBe(true)
  })

  it('forwards the agent to onRevoke', () => {
    const { onRevoke } = setup('ACTIVE')
    fireEvent.click(screen.getByText('table.revoke'))
    expect(onRevoke).toHaveBeenCalledWith(agent('ACTIVE'))
  })
})