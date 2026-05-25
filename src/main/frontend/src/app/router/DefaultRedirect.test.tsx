import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { DefaultRedirect } from './DefaultRedirect'

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))
vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

const mockUseAuth = vi.mocked(useAuth)

const baseState = {
  isInitializing: false,
  user: null,
  login: vi.fn(),
  logout: vi.fn(),
  initialize: vi.fn(),
}

beforeEach(() => vi.clearAllMocks())

describe('DefaultRedirect', () => {
  it('shows spinner while initializing instead of redirecting', () => {
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, isInitializing: true })
    )
    const { container } = render(
      <MemoryRouter>
        <DefaultRedirect />
      </MemoryRouter>
    )
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('redirects without spinner when initialization is complete and user is absent', () => {
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, isInitializing: false, user: null })
    )
    const { container } = render(
      <MemoryRouter>
        <DefaultRedirect />
      </MemoryRouter>
    )
    expect(container.querySelector('[role="status"]')).toBeNull()
  })

  it('redirects without spinner when initialization is complete and user is present', () => {
    mockUseAuth.mockImplementation((selector) =>
      selector({ ...baseState, isInitializing: false, user: { id: '1', username: 'alice', role: 'USER' as const } })
    )
    const { container } = render(
      <MemoryRouter>
        <DefaultRedirect />
      </MemoryRouter>
    )
    expect(container.querySelector('[role="status"]')).toBeNull()
  })
})