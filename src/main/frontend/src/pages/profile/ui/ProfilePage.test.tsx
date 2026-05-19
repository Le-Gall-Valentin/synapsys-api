import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAuth } from '@/features/auth'
import { ProfilePage } from './ProfilePage'

vi.mock('@/features/auth', () => ({
  useAuth: vi.fn(),
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/shared/ui', () => ({
  Button: ({ children, onClick }: { children: React.ReactNode; onClick: () => void }) => (
    <button onClick={onClick}>{children}</button>
  ),
}))

const mockUseAuth = vi.mocked(useAuth)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ProfilePage', () => {
  it('renders nothing when user is null', () => {
    mockUseAuth.mockImplementation((selector: (s: { user: null; logout: () => void }) => unknown) =>
      selector({ user: null, logout: vi.fn() })
    )
    const { container } = render(<ProfilePage />)
    expect(container.firstChild).toBeNull()
  })

  it('renders username and role when user is present', () => {
    mockUseAuth.mockImplementation(
      (selector: (s: { user: { username: string; role: string }; logout: () => void }) => unknown) =>
        selector({ user: { username: 'alice', role: 'USER' }, logout: vi.fn() })
    )
    const { getByText } = render(<ProfilePage />)
    expect(getByText('alice')).toBeDefined()
    expect(getByText('USER')).toBeDefined()
  })
})