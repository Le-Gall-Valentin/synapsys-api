import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAgentsApi } from './agentsApiContext'
import { AGENTS_QUERY_KEY } from './useAgents'
import { AGENTS_STATS_QUERY_KEY } from './useAgentStatistics'

function useInvalidateAgents() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: [AGENTS_QUERY_KEY] })
    void queryClient.invalidateQueries({ queryKey: [AGENTS_STATS_QUERY_KEY] })
  }
}

export function useRevokeAgent() {
  const api = useAgentsApi()
  const invalidate = useInvalidateAgents()
  return useMutation<void, Error, string>({
    mutationFn: (id: string) => api.revokeAgent(id),
    onSuccess: invalidate,
    onError: invalidate,
  })
}

export function useDeleteAgent() {
  const api = useAgentsApi()
  const invalidate = useInvalidateAgents()
  return useMutation<void, Error, string>({
    mutationFn: (id: string) => api.deleteAgent(id),
    onSuccess: invalidate,
    onError: invalidate,
  })
}