import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { PublicOnlyRoute } from './PublicOnlyRoute'

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))

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

describe('PublicOnlyRoute', () => {
  it('renders children when not authenticated', () => {
    mockUseAuth.mockImplementation((selector) => selector({ ...baseState, isAuthenticated: false }))
    const { getByText } = render(
      <MemoryRouter>
        <PublicOnlyRoute><div>public</div></PublicOnlyRoute>
      </MemoryRouter>
    )
    expect(getByText('public')).toBeDefined()
  })

  it('redirects and hides children when authenticated', () => {
    mockUseAuth.mockImplementation((selector) => selector({ ...baseState, isAuthenticated: true }))
    const { container } = render(
      <MemoryRouter>
        <PublicOnlyRoute><div>public</div></PublicOnlyRoute>
      </MemoryRouter>
    )
    expect(container.textContent).not.toContain('public')
  })
})