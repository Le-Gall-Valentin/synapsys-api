import { useQuery } from '@tanstack/react-query'
import { useAgentsApi } from './agentsApiContext'

export const AGENTS_STATS_QUERY_KEY = 'agents-statistics' as const

export function useAgentStatistics() {
  const api = useAgentsApi()
  return useQuery({
    queryKey: [AGENTS_STATS_QUERY_KEY],
    queryFn: () => api.getStatistics(),
  })
}