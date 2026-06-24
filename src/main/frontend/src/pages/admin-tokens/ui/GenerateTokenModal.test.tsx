import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { GenerateTokenModal } from './GenerateTokenModal'
import type { CreatedToken } from '../model/IEnrollmentTokensApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const CREATED: CreatedToken = {
  id: 't-1', serverName: 'web-01', token: 'syn_enr_secret_value',
  status: 'ACTIVE', expiresAt: '2026-06-24T12:15:00Z', createdAt: '2026-06-24T12:00:00Z',
}

describe('GenerateTokenModal', () => {
  it('submits serverName and the default TTL (15)', async () => {
    const onCreate = vi.fn().mockResolvedValue(CREATED)
    render(<GenerateTokenModal onClose={vi.fn()} onCreate={onCreate} onSuccess={vi.fn()} />)
    fireEvent.change(screen.getByLabelText('create.server_name'), { target: { value: 'web-01' } })
    fireEvent.click(screen.getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('web-01', 15))
  })

  it('reveals the clear-text token after creation', async () => {
    const onCreate = vi.fn().mockResolvedValue(CREATED)
    render(<GenerateTokenModal onClose={vi.fn()} onCreate={onCreate} onSuccess={vi.fn()} />)
    fireEvent.change(screen.getByLabelText('create.server_name'), { target: { value: 'web-01' } })
    fireEvent.click(screen.getByText('create.submit'))
    await waitFor(() => expect(screen.getByText('syn_enr_secret_value')).toBeDefined())
  })

  it('shows an error key when creation fails', async () => {
    const onCreate = vi.fn().mockRejectedValue(new Error('boom'))
    render(<GenerateTokenModal onClose={vi.fn()} onCreate={onCreate} onSuccess={vi.fn()} />)
    fireEvent.change(screen.getByLabelText('create.server_name'), { target: { value: 'web-01' } })
    fireEvent.click(screen.getByText('create.submit'))
    await waitFor(() => expect(screen.getByText('create.error.server')).toBeDefined())
  })

  it('disables submit when serverName is empty', () => {
    render(<GenerateTokenModal onClose={vi.fn()} onCreate={vi.fn()} onSuccess={vi.fn()} />)
    expect((screen.getByText('create.submit').closest('button') as HTMLButtonElement).disabled).toBe(true)
  })
})
