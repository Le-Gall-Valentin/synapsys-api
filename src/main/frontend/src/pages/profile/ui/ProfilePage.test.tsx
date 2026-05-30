import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ReactNode } from 'react'
import { useAuth } from '@/features/auth'
import { ProfilePage } from './ProfilePage'

vi.mock('@/features/auth', () => ({
  useAuth: vi.fn(),
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/shared/ui', () => ({
  Button: ({ children, onClick }: { children: ReactNode; onClick: () => void }) => (
    <button onClick={onClick}>{children}</button>
  ),
}))

const mockUseAuth = vi.mocked(useAuth)

beforeEach(() => {
  vi.clearAllMocks()
})

const baseState = {
  user: null,
  isInitializing: false,
  login: vi.fn(),
  logout: vi.fn(),
  initialize: vi.fn(),
  finalizeLogin: vi.fn(),
}

describe('ProfilePage', () => {
  it('renders nothing when user is null', () => {
    mockUseAuth.mockImplementation((selector) => selector({ ...baseState, user: null }))
    const { container } = render(<ProfilePage />)
    expect(container.firstChild).toBeNull()
  })

  it('renders username and role when user is present', () => {
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, user: { id: '1', username: 'alice', role: 'USER' as const, totpEnabled: false } })
    )
    const { getByText } = render(<ProfilePage />)
    expect(getByText('alice')).toBeDefined()
    expect(getByText('USER')).toBeDefined()
  })

  it('renders fallback initials when username is empty', () => {
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, user: { id: '1', username: '', role: 'USER' as const, totpEnabled: false } })
    )
    const { container } = render(<ProfilePage />)
    const avatar = container.querySelector('[role="img"]')
    expect(avatar?.textContent).toBe('?')
  })

  it('renders logout error alert when logout fails', async () => {
    const failingLogout = vi.fn().mockRejectedValue(new Error('Server error'))
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, user: { id: '1', username: 'alice', role: 'USER' as const, totpEnabled: false }, logout: failingLogout })
    )
    const { getByRole } = render(<ProfilePage />)

    fireEvent.click(getByRole('button'))

    await waitFor(() => {
      expect(getByRole('alert')).not.toBeNull()
      expect(getByRole('alert').textContent).toBe('error.logout_failed')
    })
  })

  it('calls logout once and shows no alert on successful logout', async () => {
    const logout = vi.fn().mockResolvedValue(undefined)
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, user: { id: '1', username: 'alice', role: 'USER' as const, totpEnabled: false }, logout })
    )
    const { getByRole, queryByRole } = render(<ProfilePage />)

    fireEvent.click(getByRole('button'))

    await waitFor(() => {
      expect(logout).toHaveBeenCalledOnce()
      expect(queryByRole('alert')).toBeNull()
    })
  })

  it('prevents double-submit — logout called only once on concurrent clicks', async () => {
    let resolveLogout!: () => void
    const pendingLogout = new Promise<void>((resolve) => { resolveLogout = resolve })
    const logout = vi.fn().mockReturnValue(pendingLogout)
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, user: { id: '1', username: 'alice', role: 'USER' as const, totpEnabled: false }, logout })
    )
    const { getByRole } = render(<ProfilePage />)
    const btn = getByRole('button')

    fireEvent.click(btn)
    fireEvent.click(btn)

    resolveLogout()
    await waitFor(() => expect(logout).toHaveBeenCalledTimes(1))
  })

})