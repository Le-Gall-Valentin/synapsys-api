import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Shield, AlertTriangle } from 'lucide-react'
import { Button } from '@/shared/ui'
import { useAuth } from '@/features/auth'
import { adminUsersApi } from '../api/adminUsersApi'
import type { AdminUser } from '../api/adminUsersApi'
import { UsersTable } from './UsersTable'
import { CreateUserModal } from './CreateUserModal'
import { EditUserRoleModal } from './EditUserRoleModal'
import { DeleteUserModal } from './DeleteUserModal'
import { ResetTotpModal } from './ResetTotpModal'

export function AdminUsersPage() {
  const { t } = useTranslation('adminUsers')
  const currentUser = useAuth(s => s.user)

  const [users, setUsers] = useState<AdminUser[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [mutationError, setMutationError] = useState(false)

  const [createOpen, setCreateOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<AdminUser | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null)
  const [resetTotpTarget, setResetTotpTarget] = useState<AdminUser | null>(null)

  const loadedRef = useRef(false)

  useEffect(() => {
    if (loadedRef.current) return
    loadedRef.current = true

    adminUsersApi.listUsers().then(setUsers).catch(() => setLoadError(true)).finally(() => setIsLoading(false))
  }, [])

  function handleToggleActive(user: AdminUser) {
    const previous = users
    setMutationError(false)
    setUsers(prev => prev.map(u => u.id === user.id ? { ...u, isActive: !u.isActive } : u))
    const call = user.isActive ? adminUsersApi.deactivateUser : adminUsersApi.activateUser
    call(user.id).catch(() => {
      setUsers(previous)
      setMutationError(true)
    })
  }

  function handleDeleteSuccess() {
    setUsers(prev => prev.filter(u => u.id !== deleteTarget!.id))
    setDeleteTarget(null)
  }

  function handleCreateSuccess() {
    setCreateOpen(false)
    setIsLoading(true)
    loadedRef.current = false
    adminUsersApi.listUsers().then(setUsers).catch(() => setLoadError(true)).finally(() => setIsLoading(false))
    loadedRef.current = true
  }

  function handleEditRoleSuccess(newRole: 'USER' | 'ADMIN') {
    if (!editTarget) return
    setUsers(prev => prev.map(u => u.id === editTarget.id ? { ...u, role: newRole } : u))
    setEditTarget(null)
  }

  function handleResetTotpSuccess() {
    if (!resetTotpTarget) return
    setUsers(prev => prev.map(u => u.id === resetTotpTarget.id ? { ...u, totpEnabled: false } : u))
    setResetTotpTarget(null)
  }

  if (!currentUser) return null

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

      {/* Mutation error */}
      {mutationError && (
        <div role="alert" className="mb-4 flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3.5 py-2.5 text-sm text-status-red">
          <AlertTriangle className="size-4 shrink-0" />
          {t('mutation_error')}
          <button
            className="ml-auto text-status-red/70 hover:text-status-red"
            onClick={() => setMutationError(false)}
            aria-label="Fermer"
          >
            ×
          </button>
        </div>
      )}

      {/* Table */}
      <UsersTable
        users={users}
        isLoading={isLoading}
        currentUser={currentUser}
        onToggleActive={handleToggleActive}
        onEditRole={setEditTarget}
        onResetTotp={setResetTotpTarget}
        onDelete={setDeleteTarget}
      />

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
          onCreate={adminUsersApi.createUser}
          onSuccess={handleCreateSuccess}
        />
      )}

      {editTarget && (
        <EditUserRoleModal
          target={editTarget}
          caller={currentUser}
          onClose={() => setEditTarget(null)}
          onUpdate={adminUsersApi.updateUserRole}
          onSuccess={handleEditRoleSuccess}
        />
      )}

      {deleteTarget && (
        <DeleteUserModal
          user={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDelete={adminUsersApi.deleteUser}
          onSuccess={handleDeleteSuccess}
        />
      )}

      {resetTotpTarget && (
        <ResetTotpModal
          user={resetTotpTarget}
          onClose={() => setResetTotpTarget(null)}
          onReset={adminUsersApi.resetTotp}
          onSuccess={handleResetTotpSuccess}
        />
      )}
    </div>
  )
}
