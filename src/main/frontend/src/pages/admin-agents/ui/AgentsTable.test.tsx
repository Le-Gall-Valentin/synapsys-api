import { fireEvent, render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AgentsTable } from './AgentsTable'
import type { Agent } from '../model/IAgentsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k, i18n: { language: 'fr' } }),
}))

const ACTIVE: Agent = { id: 'a-1', serverName: 'prod-01', ipAddress: '10.0.0.1', status: 'ACTIVE', fingerprint: 'SHA256:aa:bb:cc:dd:ee:ff:00:11:22:33:44:55', enrolledAt: '2026-02-10T09:14:00Z', lastActivityAt: '2026-06-24T10:00:00Z' }
const PENDING: Agent = { id: 'a-2', serverName: 'edge-01', ipAddress: null, status: 'PENDING', fingerprint: null, enrolledAt: '2026-06-24T09:00:00Z', lastActivityAt: null }
const REVOKED: Agent = { id: 'a-3', serverName: 'legacy', ipAddress: '10.0.0.9', status: 'REVOKED', fingerprint: 'SHA256:ff:ee:dd:cc:bb:aa:99:88:77:66:55:44', enrolledAt: '2025-01-01T00:00:00Z', lastActivityAt: '2025-09-01T00:00:00Z' }

function setup(agents: Agent[], extra = {}) {
  const onRevoke = vi.fn(); const onDelete = vi.fn(); const onSort = vi.fn()
  render(
    <AgentsTable agents={agents} isLoading={false} sortBy="enrolledAt" sortDirection="desc"
      onSort={onSort} pendingActionId={null} onRevoke={onRevoke} onDelete={onDelete} {...extra} />,
  )
  return { onRevoke, onDelete, onSort }
}

describe('AgentsTable', () => {
  it('shows Revoke only for ACTIVE/INACTIVE and Delete only for REVOKED', () => {
    setup([ACTIVE, PENDING, REVOKED])
    expect(screen.getAllByText('table.revoke')).toHaveLength(1) // ACTIVE only (PENDING has none, REVOKED has delete)
    expect(screen.getAllByText('table.delete')).toHaveLength(1) // REVOKED only
  })

  it('renders a pending-agent placeholder for a null fingerprint and a dash for null last activity', () => {
    setup([PENDING])
    expect(screen.getByText('table.fingerprint_pending')).toBeDefined()
    expect(screen.getAllByText('table.none')).toHaveLength(2)
  })

  it('calls onSort with the column field when a sortable header is clicked', () => {
    const { onSort } = setup([ACTIVE])
    fireEvent.click(screen.getByText('table.col_server'))
    expect(onSort).toHaveBeenCalledWith('serverName')
  })

  it('calls onRevoke with the agent when its revoke button is clicked', () => {
    const { onRevoke } = setup([ACTIVE])
    fireEvent.click(screen.getByText('table.revoke'))
    expect(onRevoke).toHaveBeenCalledWith(ACTIVE)
  })

  it('shows the empty state when there are no agents', () => {
    setup([])
    expect(screen.getByText('table.empty')).toBeDefined()
  })
})