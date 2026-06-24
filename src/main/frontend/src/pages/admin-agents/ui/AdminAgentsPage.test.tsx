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
  ],
  totalElements: 2, page: 0, size: 20,
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
})