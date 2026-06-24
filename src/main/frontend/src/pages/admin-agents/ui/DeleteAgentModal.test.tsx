import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { DeleteAgentModal } from './DeleteAgentModal'
import type { Agent } from '../model/IAgentsApi'
import { AgentNotRevocableError } from '../api/agentsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k, i18n: { language: 'fr' } }),
}))

const AGENT: Agent = { id: 'a-3', serverName: 'legacy', ipAddress: '10.0.0.9', status: 'REVOKED', fingerprint: 'fp', enrolledAt: '2025-01-01T00:00:00Z', lastActivityAt: '2025-09-01T00:00:00Z' }

describe('DeleteAgentModal', () => {
  it('calls onDelete then onSuccess on confirm', async () => {
    const onDelete = vi.fn().mockResolvedValue(undefined)
    const onSuccess = vi.fn()
    render(<DeleteAgentModal agent={AGENT} onClose={vi.fn()} onDelete={onDelete} onSuccess={onSuccess} />)
    fireEvent.click(screen.getByText('delete.submit'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
    expect(onDelete).toHaveBeenCalledWith('a-3')
  })

  it('shows the conflict error key when delete fails with 409', async () => {
    const onDelete = vi.fn().mockRejectedValue(new AgentNotRevocableError())
    render(<DeleteAgentModal agent={AGENT} onClose={vi.fn()} onDelete={onDelete} onSuccess={vi.fn()} />)
    fireEvent.click(screen.getByText('delete.submit'))
    await waitFor(() => expect(screen.getByText('delete.error.conflict')).toBeDefined())
  })
})