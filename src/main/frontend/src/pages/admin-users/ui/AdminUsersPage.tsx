import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Shield, AlertTriangle, ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/shared/ui'
import { useAuth } from '@/features/auth'
import { useUsers } from '../model/useUsers'
import {
  useCreateUser,
  useUpdateUserRole,
  useDeleteUser,
  useResetTotp,
  useToggleUserActive,
} from '../model/useUserMutations'
import type { AdminUser } from '../api/adminUsersApi'
import { UsersTable } from './UsersTable'
import { CreateUserModal } from './CreateUserModal'
import { EditUserRoleModal } from './EditUserRoleModal'
import { DeleteUserModal } from './DeleteUserModal'
import { ResetTotpModal } from './ResetTotpModal'

export function AdminUsersPage() {
  const { t } = useTranslation('adminUsers')
  const currentUser = useAuth(s => s.user)

  const [page, setPage] = useState(0)
  const { data, isPending, isError: loadError, isPlaceholderData } = useUsers(page)

  const [toggleError, setToggleError] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<AdminUser | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null)
  const [resetTotpTarget, setResetTotpTarget] = useState<AdminUser | null>(null)

  const createUser = useCreateUser()
  const updateUserRole = useUpdateUserRole()
  const deleteUser = useDeleteUser()
  const resetTotp = useResetTotp()
  const toggleActive = useToggleUserActive(page)

  function handleToggle(user: AdminUser) {
    setToggleError(false)
    toggleActive.mutate(user, { onError: () => setToggleError(true) })
  }

  function handleDeleteSuccess() {
    if (data && data.content.length === 1 && page > 0) {
      setPage(p => p - 1)
    }
    setDeleteTarget(null)
  }

  if (!currentUser) return null

  const users = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const totalElements = data?.totalElements ?? 0
  const showPagination = !isPending && totalPages > 1

  return (
    <div className="py-5 px-6 mx-auto">
      {/* Header */}
      <div className="flex flex-col gap-3 mb-4 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-fg-0">{t('title')}</h1>
          <p className="text-sm text-fg-2 mt-1">{t('subtitle')}</p>
        </div>
        <Button
          onClick={() => setCreateOpen(true)}
          className="self-start shrink-0 border-transparent font-semibold"
          style={{ background: 'linear-gradient(180deg, #6dead0 0%, #4dd9c2 100%)', color: '#07211c' }}
        >
          <Plus className="size-4" />
          {t('action.create')}
        </Button>
      </div>

      {/* Load error */}
      {loadError && (
        <div role="alert" className="mb-4 flex items-center gap-2.5 rounded-lg border border-status-red/25 bg-status-red-dim px-3.5 py-2.5 text-sm text-status-red">
          <AlertTriangle className="size-4 shrink-0" />
          {t('load_error')}
        </div>
      )}

      {/* Toggle/mutation error */}
      {toggleError && (
        <div role="alert" className="mb-4 flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3.5 py-2.5 text-sm text-status-red">
          <AlertTriangle className="size-4 shrink-0" />
          {t('mutation_error')}
          <button
            className="ml-auto text-status-red/70 hover:text-status-red"
            onClick={() => setToggleError(false)}
            aria-label="Fermer"
          >
            ×
          </button>
        </div>
      )}

      {/* Table */}
      <UsersTable
        users={users}
        isLoading={isPending}
        currentUser={currentUser}
        onToggleActive={handleToggle}
        onEditRole={setEditTarget}
        onResetTotp={setResetTotpTarget}
        onDelete={setDeleteTarget}
      />

      {/* Pagination */}
      {showPagination && (
        <div className={`flex items-center justify-between mt-3 transition-opacity ${isPlaceholderData ? 'opacity-50 pointer-events-none' : ''}`}>
          <span className="text-xs text-fg-2">
            {t('pagination.total', { count: totalElements })}
          </span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage(p => p - 1)}
              disabled={page === 0}
              className="size-7 flex items-center justify-center rounded-md border border-transparent text-fg-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:enabled:border-border hover:enabled:bg-bg-2 hover:enabled:text-fg-0"
              aria-label={t('pagination.prev')}
            >
              <ChevronLeft className="size-3.5" />
            </button>
            <span className="min-w-[5rem] text-center text-xs text-fg-1 tabular-nums">
              {t('pagination.page', { current: page + 1, total: totalPages })}
            </span>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={page >= totalPages - 1}
              className="size-7 flex items-center justify-center rounded-md border border-transparent text-fg-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:enabled:border-border hover:enabled:bg-bg-2 hover:enabled:text-fg-0"
              aria-label={t('pagination.next')}
            >
              <ChevronRight className="size-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* Protection rules */}
      <div className="mt-3 rounded-md border border-border bg-bg-1 p-4">
        <div className="flex gap-3">
          <Shield className="size-4 shrink-0 text-fg-3 mt-0.5" aria-hidden="true" />
          <div className="text-[12px] leading-relaxed">
            <div className="font-semibold text-fg-0 mb-2">{t('rules.title')}</div>
            <ul className="space-y-1 text-fg-2">
              <li dangerouslySetInnerHTML={{ __html: `· ${t('rules.super_admin')}` }} />
              <li dangerouslySetInnerHTML={{ __html: `· ${t('rules.self')}` }} />
              <li dangerouslySetInnerHTML={{ __html: `· ${t('rules.admin_admin')}` }} />
              <li dangerouslySetInnerHTML={{ __html: `· ${t('rules.totp')}` }} />
            </ul>
          </div>
        </div>
      </div>

      {/* Modals */}
      {createOpen && (
        <CreateUserModal
          onClose={() => setCreateOpen(false)}
          onCreate={(u, e, p, r) => createUser.mutateAsync({ username: u, email: e, password: p, role: r })}
          onSuccess={() => { setPage(0); setCreateOpen(false) }}
        />
      )}

      {editTarget && (
        <EditUserRoleModal
          target={editTarget}
          caller={currentUser}
          onClose={() => setEditTarget(null)}
          onUpdate={(id, role) => updateUserRole.mutateAsync({ id, role: role as 'USER' | 'ADMIN' })}
          onSuccess={() => setEditTarget(null)}
        />
      )}

      {deleteTarget && (
        <DeleteUserModal
          user={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDelete={deleteUser.mutateAsync}
          onSuccess={handleDeleteSuccess}
        />
      )}

      {resetTotpTarget && (
        <ResetTotpModal
          user={resetTotpTarget}
          onClose={() => setResetTotpTarget(null)}
          onReset={resetTotp.mutateAsync}
          onSuccess={() => setResetTotpTarget(null)}
        />
      )}
    </div>
  )
}
