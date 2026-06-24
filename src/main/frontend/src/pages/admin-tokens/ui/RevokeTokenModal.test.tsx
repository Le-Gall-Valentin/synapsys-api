import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { RevokeTokenModal } from './RevokeTokenModal'
import { TokenNotRevocableError } from '../api/enrollmentTokensApi'
import type { EnrollmentToken } from '../model/IEnrollmentTokensApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const TOKEN: EnrollmentToken = {
  id: 't-1', serverName: 'web-01', status: 'ACTIVE',
  expiresAt: '2026-06-24T12:15:00Z',
  createdBy: { id: 'u-1', username: 'alice' }, createdAt: '2026-06-24T12:00:00Z',
}

describe('RevokeTokenModal', () => {
  it('calls onRevoke then onSuccess on confirm', async () => {
    const onRevoke = vi.fn().mockResolvedValue(undefined)
    const onSuccess = vi.fn()
    render(<RevokeTokenModal token={TOKEN} onClose={vi.fn()} onRevoke={onRevoke} onSuccess={onSuccess} />)
    fireEvent.click(screen.getByText('revoke.submit'))
    await waitFor(() => expect(onRevoke).toHaveBeenCalledWith('t-1'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
  })

  it('shows the not_revocable error on 409', async () => {
    const onRevoke = vi.fn().mockRejectedValue(new TokenNotRevocableError())
    render(<RevokeTokenModal token={TOKEN} onClose={vi.fn()} onRevoke={onRevoke} onSuccess={vi.fn()} />)
    fireEvent.click(screen.getByText('revoke.submit'))
    await waitFor(() => expect(screen.getByText('revoke.error.not_revocable')).toBeDefined())
  })
})
