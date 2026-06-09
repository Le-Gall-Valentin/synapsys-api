import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { adminUsersApi } from '../api/adminUsersApi'

export const USERS_QUERY_KEY = 'users' as const
export const USERS_PAGE_SIZE = 20

export function useUsers(page: number, search = '') {
  const trimmed = search.trim()
  return useQuery({
    queryKey: [USERS_QUERY_KEY, page, trimmed],
    queryFn: () => adminUsersApi.listUsers(page, USERS_PAGE_SIZE, trimmed || undefined),
    placeholderData: keepPreviousData,
  })
}
