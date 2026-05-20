import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { ProtectedRoute } from './ProtectedRoute'

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))
vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

const mockUseAuth = vi.mocked(useAuth)

const baseState = {
  isAuthenticated: false,
  isInitializing: false,
  user: null,
  login: vi.fn(),
  logout: vi.fn(),
  initialize: vi.fn(),
}

beforeEach(() => vi.clearAllMocks())

describe('ProtectedRoute', () => {
  it('redirects and hides children when not authenticated', () => {
    mockUseAuth.mockImplementation((selector) => selector({ ...baseState, isAuthenticated: false }))
    const { queryByText } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(queryByText('protected')).toBeNull()
  })

  it('renders children when authenticated', () => {
    mockUseAuth.mockImplementation((selector) => selector({ ...baseState, isAuthenticated: true }))
    const { getByText } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(getByText('protected')).toBeDefined()
  })

  it('renders spinner while initializing instead of redirecting', () => {
    mockUseAuth.mockImplementation((selector) => selector({ ...baseState, isInitializing: true, isAuthenticated: false }))
    const { container } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(container.textContent).not.toContain('protected')
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })
})