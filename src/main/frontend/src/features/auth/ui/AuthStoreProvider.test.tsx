import { render, screen, waitFor, act, cleanup } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setSessionExpiredCallback, hasSessionHint } from '@/shared/lib'
import { AuthStoreProvider } from './AuthStoreProvider'

vi.mock('@/shared/lib', () => ({
  setSessionExpiredCallback: vi.fn(),
  setSessionHint: vi.fn(),
  clearSessionHint: vi.fn(),
  hasSessionHint: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

const mockedSetSessionExpiredCallback = vi.mocked(setSessionExpiredCallback)
const mockedHasSessionHint = vi.mocked(hasSessionHint)

function createApiMock() {
  return {
    login: vi.fn(),
    logout: vi.fn(),
    getMe: vi.fn(),
  }
}

beforeEach(() => {
  vi.clearAllMocks()
})

afterEach(cleanup)

describe('AuthStoreProvider', () => {
  it('renders Spinner while isInitializing', () => {
    const api = createApiMock()
    // getMe never resolves → isInitializing stays true
    api.getMe.mockReturnValue(new Promise(() => {}))
    mockedHasSessionHint.mockReturnValue(true)

    const { container } = render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    expect(container.querySelector('[role="status"]')).not.toBeNull()
    expect(screen.queryByText('content')).toBeNull()
  })

  it('renders children when not initializing', async () => {
    const api = createApiMock()
    // No session hint → initialize skips getMe and sets isInitializing: false immediately
    mockedHasSessionHint.mockReturnValue(false)

    const { container } = render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    await waitFor(() => {
      expect(container.querySelector('[role="status"]')).toBeNull()
    })
    expect(screen.queryByText('content')).not.toBeNull()
  })

  it('calls setSessionExpiredCallback with a function on mount and null on unmount', () => {
    const api = createApiMock()
    mockedHasSessionHint.mockReturnValue(false)

    const { unmount } = render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    expect(mockedSetSessionExpiredCallback).toHaveBeenCalledWith(expect.any(Function))

    unmount()

    expect(mockedSetSessionExpiredCallback).toHaveBeenLastCalledWith(null)
  })

  it('does not call api.getMe when there is no session hint', async () => {
    const api = createApiMock()
    mockedHasSessionHint.mockReturnValue(false)

    const { container } = render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    await waitFor(() => {
      expect(container.querySelector('[role="status"]')).toBeNull()
    })

    expect(api.getMe).not.toHaveBeenCalled()
  })

  it('calls api.getMe when there is a session hint', async () => {
    const api = createApiMock()
    api.getMe.mockResolvedValue({ id: '1', email: 'user@example.com' })
    mockedHasSessionHint.mockReturnValue(true)

    const { container } = render(
      <AuthStoreProvider api={api}>
        <div>content</div>
      </AuthStoreProvider>,
    )

    await waitFor(() => {
      expect(container.querySelector('[role="status"]')).toBeNull()
    })

    expect(api.getMe).toHaveBeenCalledOnce()
    expect(screen.queryByText('content')).not.toBeNull()
  })
})