import { fireEvent, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AdminAgentsPage } from './AdminAgentsPage'
import { renderWithQuery } from '@/shared/test'
import type { IAgentsApi, AgentsPage, AgentStatistics } from '../model/IAgentsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (k: string, opts?: Record<string, unknown>) => {
      if (opts && typeof opts.count === 'number') return `${k}:${opts.count}`
      if (opts && opts.current) return `${k}:${opts.current}/${opts.total}`
      if (opts && opts.server) return `${k}:${opts.server}`
      return k
    },
    i18n: { language: 'fr' },
  }),
}))

const PAGE: AgentsPage = {
  content: [
    { id: 'a-1', serverName: 'prod-01', ipAddress: '10.0.0.1', status: 'ACTIVE', fingerprint: 'SHA256:aa:bb', enrolledAt: '2026-02-10T09:14:00Z', lastActivityAt: '2026-06-24T10:00:00Z' },
    { id: 'a-2', serverName: 'edge-01', ipAddress: null, status: 'PENDING', fingerprint: null, enrolledAt: '2026-06-24T09:00:00Z', lastActivityAt: null },
    { id: 'a-3', serverName: 'legacy', ipAddress: '10.0.0.9', status: 'REVOKED', fingerprint: 'SHA256:cc:dd', enrolledAt: '2025-01-01T00:00:00Z', lastActivityAt: '2025-09-01T00:00:00Z' },
  ],
  totalElements: 3, page: 0, size: 20,
}
const STATS: AgentStatistics = { active: 1, inactive: 0, pending: 1, revoked: 0, total: 2 }

function makeApi(overrides: Partial<IAgentsApi> = {}): IAgentsApi {
  return {
    listAgents: vi.fn().mockResolvedValue(PAGE),
    getStatistics: vi.fn().mockResolvedValue(STATS),
    revokeAgent: vi.fn().mockResolvedValue(undefined),
    deleteAgent: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

function renderPage(api: IAgentsApi) {
  return renderWithQuery(<MemoryRouter><AdminAgentsPage api={api} /></MemoryRouter>)
}

beforeEach(() => vi.clearAllMocks())

describe('AdminAgentsPage', () => {
  it('renders rows returned by the API', async () => {
    renderPage(makeApi())
    await waitFor(() => expect(screen.getAllByText('prod-01').length).toBeGreaterThanOrEqual(1))
    expect(screen.getAllByText('edge-01').length).toBeGreaterThanOrEqual(1)
  })

  it('shows a load error alert when the list fails', async () => {
    renderPage(makeApi({ listAgents: vi.fn().mockRejectedValue(new Error('boom')) }))
    await waitFor(() => expect(screen.getByText('load_error')).toBeDefined())
  })

  it('passes the debounced search term to the API', async () => {
    const api = makeApi()
    renderPage(api)
    await waitFor(() => expect(screen.getAllByText('prod-01').length).toBeGreaterThanOrEqual(1))
    fireEvent.change(screen.getByPlaceholderText('search.placeholder'), { target: { value: 'edge' } })
    await waitFor(() =>
      expect(api.listAgents).toHaveBeenLastCalledWith(0, 20, 'enrolledAt', 'desc', 'edge'),
    )
  })

  it('links the add-server CTA to the tokens route', async () => {
    renderPage(makeApi())
    await waitFor(() => expect(screen.getAllByText('prod-01').length).toBeGreaterThanOrEqual(1))
    expect(screen.getByText('action.add_server').closest('a')?.getAttribute('href')).toContain('/administration/tokens')
  })

  it('selects a new sort field descending, then toggles its direction on a second click', async () => {
    const api = makeApi()
    renderPage(api)
    // Wait for the rows (and thus the table headers) to render, not just the first call.
    await waitFor(() => expect(screen.getAllByText('prod-01').length).toBeGreaterThanOrEqual(1))
    expect(api.listAgents).toHaveBeenLastCalledWith(0, 20, 'enrolledAt', 'desc', undefined)
    // First click on an inactive column selects it, defaulting to descending.
    fireEvent.click(screen.getByText('table.col_server'))
    await waitFor(() => expect(api.listAgents).toHaveBeenLastCalledWith(0, 20, 'serverName', 'desc', undefined))
    // Second click on the active column flips the direction to ascending.
    fireEvent.click(screen.getByText('table.col_server'))
    await waitFor(() => expect(api.listAgents).toHaveBeenLastCalledWith(0, 20, 'serverName', 'asc', undefined))
  })

  it('opens the confirm modal from an ACTIVE row and revokes on confirm', async () => {
    const api = makeApi()
    renderPage(api)
    await waitFor(() => expect(screen.getAllByText('prod-01').length).toBeGreaterThanOrEqual(1))
    fireEvent.click(screen.getAllByText('table.revoke')[0])
    fireEvent.click(screen.getByText('revoke.submit'))
    await waitFor(() => expect(api.revokeAgent).toHaveBeenCalledWith('a-1'))
    // Success closes the modal.
    await waitFor(() => expect(screen.queryByText('revoke.submit')).toBeNull())
  })

  it('opens the confirm modal from a REVOKED row and deletes on confirm', async () => {
    const api = makeApi()
    renderPage(api)
    await waitFor(() => expect(screen.getAllByText('legacy').length).toBeGreaterThanOrEqual(1))
    fireEvent.click(screen.getAllByText('table.delete')[0])
    fireEvent.click(screen.getByText('delete.submit'))
    await waitFor(() => expect(api.deleteAgent).toHaveBeenCalledWith('a-3'))
    await waitFor(() => expect(screen.queryByText('delete.submit')).toBeNull())
  })
})