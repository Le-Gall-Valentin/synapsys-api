import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { useAuthGuard } from './useAuthGuard'
import { DefaultRedirect } from './DefaultRedirect'

vi.mock('./useAuthGuard')

const mockUseAuthGuard = vi.mocked(useAuthGuard)

beforeEach(() => vi.clearAllMocks())

describe('DefaultRedirect', () => {
  it('shows spinner while initializing instead of redirecting', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: true, isAuthenticated: false, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <DefaultRedirect />
      </MemoryRouter>
    )
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('redirects without spinner when initialization is complete and user is absent', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <DefaultRedirect />
      </MemoryRouter>
    )
    expect(container.querySelector('[role="status"]')).toBeNull()
  })

  it('redirects without spinner when initialization is complete and user is present', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <DefaultRedirect />
      </MemoryRouter>
    )
    expect(container.querySelector('[role="status"]')).toBeNull()
  })
})