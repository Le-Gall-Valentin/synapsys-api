import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button } from '@/shared/ui'
import { NetworkError, RateLimitError } from '@/shared/lib'
import type { User } from '@/entities/user'
import { RoleAlreadyAssignedError } from '../api/adminUsersApi'
import type { AdminUser } from '../api/adminUsersApi'

interface EditUserRoleModalProps {
  target: AdminUser
  caller: User
  onClose: () => void
  onUpdate: (id: string, role: 'USER' | 'ADMIN') => Promise<void>
  onSuccess: (newRole: 'USER' | 'ADMIN') => void
}

export function EditUserRoleModal({ target, caller, onClose, onUpdate, onSuccess }: EditUserRoleModalProps) {
  const { t } = useTranslation('adminUsers')

  const availableRoles: Array<'USER' | 'ADMIN'> =
    caller.role === 'SUPER_ADMIN' ? ['USER', 'ADMIN'] : ['USER']

  const initialRole: 'USER' | 'ADMIN' =
    target.role === 'SUPER_ADMIN' ? 'USER' : (target.role as 'USER' | 'ADMIN')

  const [role, setRole] = useState<'USER' | 'ADMIN'>(initialRole)
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onUpdate(target.id, role)
      onSuccess(role)
    } catch (error) {
      if (error instanceof RoleAlreadyAssignedError) {
        setErrorKey('edit_role.error.already_assigned')
      } else if (error instanceof RateLimitError) {
        setErrorKey('edit_role.error.rate_limit')
      } else if (error instanceof NetworkError) {
        setErrorKey('edit_role.error.network')
      } else {
        setErrorKey('edit_role.error.server')
      }
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    setErrorKey(null)
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('edit_role.title', { username: target.username })}>
      <div className="mb-5">
        <h3 className="text-base font-semibold text-fg-0">
          {t('edit_role.title', { username: target.username })}
        </h3>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="mb-1 flex flex-col gap-2">
          <label htmlFor="edit-role" className="text-xs font-medium text-fg-1">
            {t('edit_role.role')}
          </label>
          <select
            id="edit-role"
            value={role}
            onChange={e => setRole(e.target.value as 'USER' | 'ADMIN')}
            disabled={isLoading}
            className="w-full rounded-lg border border-border bg-bg-1 px-3.5 py-3 text-sm text-fg-0 outline-none transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_var(--color-accent-ring)] disabled:opacity-50"
          >
            {availableRoles.map(r => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>
        </div>

        {errorKey && (
          <div role="alert" className="mt-3 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red">
            {t(errorKey)}
          </div>
        )}

        <div className="flex justify-end gap-2 mt-5">
          <Button type="button" onClick={handleClose} disabled={isLoading}>
            {t('edit_role.cancel')}
          </Button>
          <Button
            type="submit"
            isLoading={isLoading}
            className="border-transparent font-semibold"
            style={{ background: 'linear-gradient(180deg, #6dead0 0%, #4dd9c2 100%)', color: '#07211c' }}
          >
            {t('edit_role.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
