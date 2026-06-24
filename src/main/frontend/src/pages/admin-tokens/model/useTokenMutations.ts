import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { CreatedToken } from './IEnrollmentTokensApi'
import { useEnrollmentTokensApi } from './enrollmentTokensApiContext'
import { TOKENS_QUERY_KEY } from './useEnrollmentTokens'

function useInvalidateTokens() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: [TOKENS_QUERY_KEY] })
}

export function useCreateToken() {
  const api = useEnrollmentTokensApi()
  const invalidate = useInvalidateTokens()
  return useMutation<CreatedToken, Error, { serverName: string; ttlMinutes?: number }>({
    mutationFn: ({ serverName, ttlMinutes }) => api.createToken(serverName, ttlMinutes),
    onSuccess: invalidate,
  })
}

export function useRevokeToken() {
  const api = useEnrollmentTokensApi()
  const invalidate = useInvalidateTokens()
  return useMutation<void, Error, string>({
    mutationFn: (id: string) => api.revokeToken(id),
    onSuccess: invalidate,
  })
}
