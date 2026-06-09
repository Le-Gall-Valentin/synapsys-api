import { useMutation, useQueryClient } from '@tanstack/react-query'
import { adminUsersApi } from '../api/adminUsersApi'
import type { UsersPage, AdminUser } from '../api/adminUsersApi'
import { USERS_QUERY_KEY } from './useUsers'

function useInvalidateUsers() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
}

export function useCreateUser() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ username, email, password, role }: {
      username: string
      email: string
      password: string
      role: 'USER' | 'ADMIN'
    }) => adminUsersApi.createUser(username, email, password, role),
    onSuccess: invalidate,
  })
}

export function useUpdateUserRole() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: 'USER' | 'ADMIN' }) =>
      adminUsersApi.updateUserRole(id, role),
    onSuccess: invalidate,
  })
}

export function useDeleteUser() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: (id: string) => adminUsersApi.deleteUser(id),
    onSuccess: invalidate,
  })
}

export function useResetTotp() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: (id: string) => adminUsersApi.resetTotp(id),
    onSuccess: invalidate,
  })
}

export function useToggleUserActive(page: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (user: AdminUser) =>
      user.isActive
        ? adminUsersApi.deactivateUser(user.id)
        : adminUsersApi.activateUser(user.id),
    onMutate: async (user: AdminUser) => {
      await queryClient.cancelQueries({ queryKey: [USERS_QUERY_KEY, page] })
      const previous = queryClient.getQueryData<UsersPage>([USERS_QUERY_KEY, page])
      queryClient.setQueryData<UsersPage>([USERS_QUERY_KEY, page], old => {
        if (!old) return old
        return {
          ...old,
          content: old.content.map(u =>
            u.id === user.id ? { ...u, isActive: !u.isActive } : u
          ),
        }
      })
      return { previous }
    },
    onError: (_err, _user, context) => {
      if (context?.previous) {
        queryClient.setQueryData([USERS_QUERY_KEY, page], context.previous)
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY, page] }),
  })
}
