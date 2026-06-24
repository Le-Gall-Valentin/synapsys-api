import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { describe, it, expect, vi } from 'vitest'
import { createTestQueryClient } from '@/shared/test'
import type { IAgentsApi, AgentsPage } from './IAgentsApi'
import { AdminAgentsApiProvider } from './agentsApiContext'
import { useAgentsPage } from './useAgentsPage'

// One agent per page: deleting the last row of a page beyond the first must step back.
const ONE: AgentsPage = {
  content: [{ id: 'a-1', serverName: 'srv', ipAddress: null, status: 'REVOKED', fingerprint: null, enrolledAt: '2026-01-01T00:00:00Z', lastActivityAt: null }],
  totalElements: 21, page: 0, size: 20,
}

function makeApi(): IAgentsApi {
  return {
    listAgents: vi.fn().mockResolvedValue(ONE),
    getStatistics: vi.fn().mockResolvedValue({ active: 0, inactive: 0, pending: 0, revoked: 1, total: 1 }),
    revokeAgent: vi.fn().mockResolvedValue(undefined),
    deleteAgent: vi.fn().mockResolvedValue(undefined),
  }
}

function wrapper(api: IAgentsApi) {
  const client = createTestQueryClient()
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>
      <AdminAgentsApiProvider api={api}>{children}</AdminAgentsApiProvider>
    </QueryClientProvider>
  )
}

describe('useAgentsPage', () => {
  it('toggleSort selects a new field descending, then flips direction on the same field', () => {
    const { result } = renderHook(() => useAgentsPage(), { wrapper: wrapper(makeApi()) })
    act(() => result.current.toggleSort('serverName'))
    expect(result.current.sortBy).toBe('serverName')
    expect(result.current.sortDirection).toBe('desc')
    act(() => result.current.toggleSort('serverName'))
    expect(result.current.sortDirection).toBe('asc')
  })

  it('changeSearch resets the page to 0', async () => {
    const { result } = renderHook(() => useAgentsPage(), { wrapper: wrapper(makeApi()) })
    await waitFor(() => expect(result.current.isPending).toBe(false))
    act(() => result.current.setPage(1))
    await waitFor(() => expect(result.current.page).toBe(1))
    act(() => result.current.changeSearch('srv'))
    expect(result.current.page).toBe(0)
  })

  it('onDeleteSuccess steps back off an emptied page; onRevokeSuccess leaves the page', async () => {
    const { result } = renderHook(() => useAgentsPage(), { wrapper: wrapper(makeApi()) })
    await waitFor(() => expect(result.current.isPending).toBe(false))

    act(() => result.current.setPage(1))
    await waitFor(() => expect(result.current.page).toBe(1))
    await waitFor(() => expect(result.current.isPlaceholderData).toBe(false))
    act(() => result.current.onDeleteSuccess())
    expect(result.current.page).toBe(0) // last row of page 1 removed -> step back

    act(() => result.current.setPage(1))
    await waitFor(() => expect(result.current.page).toBe(1))
    act(() => result.current.onRevokeSuccess())
    expect(result.current.page).toBe(1) // revoke keeps the row listed -> no page change
  })
})