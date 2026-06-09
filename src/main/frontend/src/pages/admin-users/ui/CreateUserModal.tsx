import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input } from '@/shared/ui'
import { NetworkError, RateLimitError } from '@/shared/lib'
import { ConflictError } from '../api/adminUsersApi'

interface CreateUserModalProps {
  onClose: () => void
  onCreate: (username: string, email: string, password: string, role: 'USER' | 'ADMIN') => Promise<void>
  onSuccess: () => void
}

export function CreateUserModal({ onClose, onCreate, onSuccess }: CreateUserModalProps) {
  const { t } = useTranslation('adminUsers')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'USER' | 'ADMIN'>('USER')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  const canSubmit = username.trim().length > 0 && email.trim().length > 0 && password.length > 0

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onCreate(username.trim(), email.trim(), password, role)
      onSuccess()
    } catch (error) {
      if (error instanceof ConflictError) {
        setErrorKey('create.error.conflict')
      } else if (error instanceof RateLimitError) {
        setErrorKey('create.error.rate_limit')
      } else if (error instanceof NetworkError) {
        setErrorKey('create.error.network')
      } else {
        setErrorKey('create.error.server')
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
    <Dialog open onClose={handleClose} title={t('create.title')} maxWidth="max-w-lg">
      <div className="mb-5">
        <h3 className="text-base font-semibold text-fg-0">{t('create.title')}</h3>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="flex flex-col gap-3 sm:flex-row mb-3">
          <div className="min-w-0 flex-1">
            <Input
              label={t('create.username')}
              name="username"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder={t('create.username_placeholder')}
              disabled={isLoading}
              autoFocus
            />
          </div>
          <div className="min-w-0 flex-1">
            <Input
              label={t('create.email')}
              name="email"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder={t('create.email_placeholder')}
              disabled={isLoading}
            />
          </div>
        </div>

        <div className="mb-3">
          <Input
            label={t('create.password')}
            name="password"
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            disabled={isLoading}
          />
        </div>

        <div className="mb-1 flex flex-col gap-2">
          <label htmlFor="create-role" className="text-xs font-medium text-fg-1">
            {t('create.role')}
          </label>
          <select
            id="create-role"
            value={role}
            onChange={e => setRole(e.target.value as 'USER' | 'ADMIN')}
            disabled={isLoading}
            className="w-full rounded-lg border border-border bg-bg-1 px-3.5 py-3 text-sm text-fg-0 outline-none transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_var(--color-accent-ring)] disabled:opacity-50"
          >
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <p className="text-[11px] text-fg-3">{t('create.role_note')}</p>
        </div>

        {errorKey && (
          <div role="alert" className="mt-3 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red">
            {t(errorKey)}
          </div>
        )}

        <div className="flex justify-end gap-2 mt-5">
          <Button type="button" onClick={handleClose} disabled={isLoading}>
            {t('create.cancel')}
          </Button>
          <Button
            type="submit"
            disabled={!canSubmit}
            isLoading={isLoading}
            className="border-transparent font-semibold"
            style={{ background: 'linear-gradient(180deg, #6dead0 0%, #4dd9c2 100%)', color: '#07211c' }}
          >
            {t('create.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
