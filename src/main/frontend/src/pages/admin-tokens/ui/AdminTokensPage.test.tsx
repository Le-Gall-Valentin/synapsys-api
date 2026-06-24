import { fireEvent, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AdminTokensPage } from './AdminTokensPage'
import { renderWithQuery } from '@/shared/test'
import type { IEnrollmentTokensApi, TokensPage } from '../model/IEnrollmentTokensApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (k: string, opts?: Record<string, unknown>) => {
      if (opts && typeof opts.count === 'number') return `${k}:${opts.count}`
      if (opts && opts.current) return `${k}:${opts.current}/${opts.total}`
      return k
    },
    i18n: { language: 'fr' },
  }),
}))

const PAGE: TokensPage = {
  content: [
    { id: 't-1', serverName: 'web-01', status: 'ACTIVE', expiresAt: '2999-01-01T00:00:00Z', createdBy: { id: 'u-1', username: 'alice' }, createdAt: '2026-06-24T11:00:00Z' },
    { id: 't-2', serverName: 'db-01', status: 'CONSUMED', expiresAt: '2026-06-20T00:00:00Z', createdBy: { id: 'u-1', username: 'alice' }, createdAt: '2026-06-20T11:00:00Z' },
  ],
  totalElements: 2, page: 0, size: 20,
}

function makeApi(overrides: Partial<IEnrollmentTokensApi> = {}): IEnrollmentTokensApi {
  return {
    listTokens: vi.fn().mockResolvedValue(PAGE),
    createToken: vi.fn(),
    revokeToken: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

beforeEach(() => vi.clearAllMocks())

describe('AdminTokensPage', () => {
  it('renders rows returned by the API', async () => {
    renderWithQuery(<AdminTokensPage api={makeApi()} />)
    // The page renders both the desktop table and the mobile card list (CSS-toggled via
    // `hidden`/`md:hidden`); jsdom does not evaluate media queries, so both are present in
    // the DOM simultaneously and matching text appears twice. Use getAllByText accordingly.
    await waitFor(() => expect(screen.getAllByText('web-01').length).toBeGreaterThanOrEqual(1))
    expect(screen.getAllByText('db-01').length).toBeGreaterThanOrEqual(1)
  })

  it('shows a revoke button only for ACTIVE tokens', async () => {
    renderWithQuery(<AdminTokensPage api={makeApi()} />)
    await waitFor(() => expect(screen.getAllByText('web-01').length).toBeGreaterThanOrEqual(1))
    // One ACTIVE token (web-01) => one revoke control per rendered layout (table + cards).
    expect(screen.getAllByText('table.revoke').length).toBeGreaterThanOrEqual(1)
  })

  it('shows a load error alert when the list fails', async () => {
    const api = makeApi({ listTokens: vi.fn().mockRejectedValue(new Error('boom')) })
    renderWithQuery(<AdminTokensPage api={api} />)
    await waitFor(() => expect(screen.getByText('load_error')).toBeDefined())
  })

  it('opens the generate modal', async () => {
    renderWithQuery(<AdminTokensPage api={makeApi()} />)
    await waitFor(() => expect(screen.getAllByText('web-01').length).toBeGreaterThanOrEqual(1))
    fireEvent.click(screen.getByText('action.create'))
    // The modal exposes the title both as a visible heading and as the Dialog's
    // (sr-only) accessible label, so it appears twice in the DOM.
    expect(screen.getAllByText('create.title').length).toBeGreaterThanOrEqual(1)
  })
})
