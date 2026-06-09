import { useTranslation } from 'react-i18next'
import { Key, Pencil, Trash2 } from 'lucide-react'
import type { User, AdminUser } from '@/entities/user'
import { RolePill, UserAvatar } from '@/entities/user'
import { canDeactivate, canActivate, canDelete, canResetTotp, canEditRole } from '../lib/permissions'
import type { PermissionResult } from '../lib/permissions'

interface UsersTableProps {
  users: AdminUser[]
  isLoading: boolean
  currentUser: User
  onToggleActive: (user: AdminUser) => void
  onEditRole: (user: AdminUser) => void
  onResetTotp: (user: AdminUser) => void
  onDelete: (user: AdminUser) => void
}

export function UsersTable({
  users,
  isLoading,
  currentUser,
  onToggleActive,
  onEditRole,
  onResetTotp,
  onDelete,
}: UsersTableProps) {
  const { t, i18n } = useTranslation('adminUsers')

  if (isLoading) {
    return (
      <div className="rounded-md border border-border bg-bg-1 overflow-hidden">
        <div className="divide-y divide-border">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-4 py-3">
              <div className="size-6 shrink-0 rounded-md bg-bg-3 animate-pulse" />
              <div className="h-3.5 w-28 bg-bg-3 animate-pulse rounded" />
              <div className="h-3 w-36 bg-bg-3 animate-pulse rounded ml-auto" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-md border border-border bg-bg-1 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-bg-2">
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_user')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_email')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_role')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_status')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_totp')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_created')}</th>
              <th className="px-4 py-2.5" />
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {users.map(user => (
              <UserRow
                key={user.id}
                user={user}
                currentUser={currentUser}
                t={t}
                i18nLang={i18n.language}
                onToggleActive={onToggleActive}
                onEditRole={onEditRole}
                onResetTotp={onResetTotp}
                onDelete={onDelete}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

interface RowProps {
  user: AdminUser
  currentUser: User
  t: (key: string, opts?: Record<string, unknown>) => string
  i18nLang: string
  onToggleActive: (user: AdminUser) => void
  onEditRole: (user: AdminUser) => void
  onResetTotp: (user: AdminUser) => void
  onDelete: (user: AdminUser) => void
}

function denialTitle(check: PermissionResult, t: RowProps['t'], fallback: string): string {
  return check.ok ? fallback : t(`perm.${check.reason}`)
}

function UserRow({ user, currentUser, t, i18nLang, onToggleActive, onEditRole, onResetTotp, onDelete }: RowProps) {
  const isMe = user.id === currentUser.id

  const toggleCheck = user.isActive
    ? canDeactivate(currentUser, user)
    : canActivate(currentUser, user)
  const deleteCheck = canDelete(currentUser, user)
  const totpCheck = canResetTotp(currentUser, user)
  const editCheck = canEditRole(currentUser, user)

  const toggleLabel = user.isActive ? t('table.toggle_deactivate') : t('table.toggle_activate')

  const createdDate = new Date(user.createdAt).toLocaleDateString(
    i18nLang === 'fr' ? 'fr-FR' : 'en-GB',
    { day: '2-digit', month: 'short', year: 'numeric' },
  )

  return (
    <tr className={isMe ? 'bg-accent/[0.02]' : undefined}>
      {/* Username */}
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <UserAvatar username={user.username} role={user.role} />
          <span className="font-medium text-fg-0">{user.username}</span>
          {isMe && (
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-bg-3 text-fg-2 font-mono">
              {t('table.you')}
            </span>
          )}
        </div>
      </td>

      {/* Email */}
      <td className="px-4 py-3 font-mono text-xs text-fg-2">{user.email}</td>

      {/* Role */}
      <td className="px-4 py-3">
        <RolePill role={user.role} />
      </td>

      {/* Status */}
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <button
            role="switch"
            aria-checked={user.isActive}
            aria-label={toggleLabel}
            title={denialTitle(toggleCheck, t, toggleLabel)}
            onClick={() => toggleCheck.ok && onToggleActive(user)}
            disabled={!toggleCheck.ok}
            className={`relative inline-flex h-4 w-7 shrink-0 items-center rounded-full border transition-colors
              ${user.isActive ? 'bg-status-green border-status-green/20' : 'bg-bg-4 border-border-2'}
              ${!toggleCheck.ok ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
          >
            <span className={`pointer-events-none inline-block size-3 rounded-full bg-white shadow-sm transition-transform ${user.isActive ? 'translate-x-3.5' : 'translate-x-0.5'}`} />
          </button>
          <span className="text-xs text-fg-2">
            {user.isActive ? t('table.active') : t('table.inactive')}
          </span>
        </div>
      </td>

      {/* 2FA */}
      <td className="px-4 py-3">
        {user.totpEnabled ? (
          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full border border-status-green/20 bg-status-green-dim text-[11px] font-medium text-status-green">
            <span className="size-1.5 rounded-full bg-status-green" aria-hidden="true" />
            {t('table.totp_on')}
          </span>
        ) : (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full border border-border bg-bg-2 text-[11px] font-medium text-fg-2">
            {t('table.totp_off')}
          </span>
        )}
      </td>

      {/* Created */}
      <td className="px-4 py-3 font-mono text-[11px] text-fg-2">{createdDate}</td>

      {/* Actions */}
      <td className="px-4 py-3">
        <div className="flex items-center justify-end gap-1">
          <ActionButton
            label={t('table.btn_reset_totp', { username: user.username })}
            check={totpCheck}
            t={t}
            onClick={() => onResetTotp(user)}
          >
            <Key className="size-3.5" />
          </ActionButton>
          <ActionButton
            label={t('table.btn_edit', { username: user.username })}
            check={editCheck}
            t={t}
            onClick={() => onEditRole(user)}
          >
            <Pencil className="size-3.5" />
          </ActionButton>
          <ActionButton
            label={t('table.btn_delete')}
            check={deleteCheck}
            t={t}
            onClick={() => onDelete(user)}
          >
            <Trash2 className="size-3.5" />
          </ActionButton>
        </div>
      </td>
    </tr>
  )
}

interface ActionButtonProps {
  label: string
  check: PermissionResult
  t: RowProps['t']
  onClick: () => void
  children: React.ReactNode
}

function ActionButton({ label, check, t, onClick, children }: ActionButtonProps) {
  return (
    <button
      aria-label={label}
      title={denialTitle(check, t, label)}
      disabled={!check.ok}
      onClick={onClick}
      className={`size-7 flex items-center justify-center rounded-md border border-transparent text-fg-2 transition-colors
        ${!check.ok
          ? 'opacity-40 cursor-not-allowed'
          : 'hover:border-border hover:bg-bg-2 hover:text-fg-0'
        }`}
    >
      {children}
    </button>
  )
}
