import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { adminUsersApi } from '../api/adminUsersApi'

export const USERS_QUERY_KEY = 'users' as const
export const USERS_PAGE_SIZE = 20

export function useUsers(page: number) {
  return useQuery({
    queryKey: [USERS_QUERY_KEY, page],
    queryFn: () => adminUsersApi.listUsers(page, USERS_PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
}
