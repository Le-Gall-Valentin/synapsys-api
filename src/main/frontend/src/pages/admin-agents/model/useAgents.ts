import { useQuery, keepPreviousData } from '@tanstack/react-query'
import type { AgentSortField, SortDirection } from './IAgentsApi'
import { useAgentsApi } from './agentsApiContext'

export const AGENTS_QUERY_KEY = 'agents' as const
export const AGENTS_PAGE_SIZE = 20

export function useAgents(page: number, sortBy: AgentSortField, sortDirection: SortDirection, search: string) {
  const api = useAgentsApi()
  return useQuery({
    queryKey: [AGENTS_QUERY_KEY, page, sortBy, sortDirection, search],
    queryFn: () => api.listAgents(page, AGENTS_PAGE_SIZE, sortBy, sortDirection, search || undefined),
    placeholderData: keepPreviousData,
  })
}