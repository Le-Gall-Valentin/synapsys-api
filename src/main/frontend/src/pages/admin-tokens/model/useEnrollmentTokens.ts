import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useEnrollmentTokensApi } from './enrollmentTokensApiContext'

export const TOKENS_QUERY_KEY = 'enrollment-tokens' as const
export const TOKENS_PAGE_SIZE = 20

export function useEnrollmentTokens(page: number) {
  const api = useEnrollmentTokensApi()
  return useQuery({
    queryKey: [TOKENS_QUERY_KEY, page],
    queryFn: () => api.listTokens(page, TOKENS_PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
}
