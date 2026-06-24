import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { RevokeAgentModal } from './RevokeAgentModal'
import type { Agent } from '../model/IAgentsApi'
import { AgentNotRevocableError } from '../api/agentsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k, i18n: { language: 'fr' } }),
}))

const AGENT: Agent = { id: 'a-1', serverName: 'prod-01', ipAddress: '10.0.0.1', status: 'ACTIVE', fingerprint: 'fp', enrolledAt: '2026-02-10T09:14:00Z', lastActivityAt: '2026-06-24T10:00:00Z' }

describe('RevokeAgentModal', () => {
  it('calls onRevoke then onSuccess on confirm', async () => {
    const onRevoke = vi.fn().mockResolvedValue(undefined)
    const onSuccess = vi.fn()
    render(<RevokeAgentModal agent={AGENT} onClose={vi.fn()} onRevoke={onRevoke} onSuccess={onSuccess} />)
    fireEvent.click(screen.getByText('revoke.submit'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
    expect(onRevoke).toHaveBeenCalledWith('a-1')
  })

  it('shows the conflict error key when revoke fails with 409', async () => {
    const onRevoke = vi.fn().mockRejectedValue(new AgentNotRevocableError())
    render(<RevokeAgentModal agent={AGENT} onClose={vi.fn()} onRevoke={onRevoke} onSuccess={vi.fn()} />)
    fireEvent.click(screen.getByText('revoke.submit'))
    await waitFor(() => expect(screen.getByText('revoke.error.conflict')).toBeDefined())
  })
})