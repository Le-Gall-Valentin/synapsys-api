import { render } from '@testing-library/react'
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
  isInitializing: false,
  login: vi.fn(),
  logout: vi.fn(),
  initialize: vi.fn(),
  getMe: vi.fn(),
}

describe('ProfilePage', () => {
  it('renders nothing when user is null', () => {
    mockUseAuth.mockImplementation((selector) => selector({ ...baseState, user: null }))
    const { container } = render(<ProfilePage />)
    expect(container.firstChild).toBeNull()
  })

  it('renders username and role when user is present', () => {
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, user: { id: '1', username: 'alice', role: 'USER' as const } })
    )
    const { getByText } = render(<ProfilePage />)
    expect(getByText('alice')).toBeDefined()
    expect(getByText('USER')).toBeDefined()
  })

  it('renders fallback initials when username is empty', () => {
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, user: { id: '1', username: '', role: 'USER' as const } })
    )
    const { container } = render(<ProfilePage />)
    const avatar = container.querySelector('[role="img"]')
    expect(avatar?.textContent).toBe('?')
  })

})