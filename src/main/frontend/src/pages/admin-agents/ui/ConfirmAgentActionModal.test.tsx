import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ConfirmAgentActionModal } from './ConfirmAgentActionModal'
import type { Agent } from '../model/IAgentsApi'
import { AgentNotRevocableError } from '../api/agentsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k, i18n: { language: 'fr' } }),
}))

const AGENT: Agent = { id: 'a-1', serverName: 'prod-01', ipAddress: '10.0.0.1', status: 'ACTIVE', fingerprint: 'fp', enrolledAt: '2026-02-10T09:14:00Z', lastActivityAt: '2026-06-24T10:00:00Z' }

describe('ConfirmAgentActionModal', () => {
  it('calls onConfirm then onSuccess on confirm (revoke)', async () => {
    const onConfirm = vi.fn().mockResolvedValue(undefined)
    const onSuccess = vi.fn()
    render(<ConfirmAgentActionModal agent={AGENT} action="revoke" onClose={vi.fn()} onConfirm={onConfirm} onSuccess={onSuccess} />)
    fireEvent.click(screen.getByText('revoke.submit'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
    expect(onConfirm).toHaveBeenCalledWith('a-1')
  })

  it('uses the delete i18n prefix for the delete action', () => {
    render(<ConfirmAgentActionModal agent={AGENT} action="delete" onClose={vi.fn()} onConfirm={vi.fn()} onSuccess={vi.fn()} />)
    expect(screen.getByText('delete.submit')).toBeDefined()
    expect(screen.getByText('delete.body')).toBeDefined()
  })

  it('maps a 409 to the revoke conflict key', async () => {
    const onConfirm = vi.fn().mockRejectedValue(new AgentNotRevocableError())
    render(<ConfirmAgentActionModal agent={AGENT} action="revoke" onClose={vi.fn()} onConfirm={onConfirm} onSuccess={vi.fn()} />)
    fireEvent.click(screen.getByText('revoke.submit'))
    await waitFor(() => expect(screen.getByText('revoke.error.conflict')).toBeDefined())
  })

  it('maps a 409 to the delete conflict key', async () => {
    const onConfirm = vi.fn().mockRejectedValue(new AgentNotRevocableError())
    render(<ConfirmAgentActionModal agent={AGENT} action="delete" onClose={vi.fn()} onConfirm={onConfirm} onSuccess={vi.fn()} />)
    fireEvent.click(screen.getByText('delete.submit'))
    await waitFor(() => expect(screen.getByText('delete.error.conflict')).toBeDefined())
  })
})