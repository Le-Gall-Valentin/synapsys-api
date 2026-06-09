import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, Key } from 'lucide-react'
import { Dialog, Button } from '@/shared/ui'
import { NetworkError, RateLimitError } from '@/shared/lib'
import type { AdminUser } from '../api/adminUsersApi'

interface ResetTotpModalProps {
  user: AdminUser
  onClose: () => void
  onReset: (id: string) => Promise<void>
  onSuccess: () => void
}

export function ResetTotpModal({ user, onClose, onReset, onSuccess }: ResetTotpModalProps) {
  const { t } = useTranslation('adminUsers')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  async function handleSubmit() {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onReset(user.id)
      onSuccess()
    } catch (error) {
      if (error instanceof RateLimitError) {
        setErrorKey('reset_totp.error.rate_limit')
      } else if (error instanceof NetworkError) {
        setErrorKey('reset_totp.error.network')
      } else {
        setErrorKey('reset_totp.error.server')
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
    <Dialog open onClose={handleClose} title={t('reset_totp.title', { username: user.username })} maxWidth="max-w-[460px]">
      <div className="mb-4">
        <h3 className="text-base font-semibold text-fg-0 mb-1">
          {t('reset_totp.title', { username: user.username })}
        </h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('reset_totp.body')}</p>
      </div>

      <div className="mb-5 flex items-start gap-2.5 rounded-lg border border-status-orange/25 bg-status-orange-dim px-3.5 py-2.5 text-sm text-status-orange">
        <AlertTriangle className="size-3.5 shrink-0 mt-0.5" />
        <span>{t('reset_totp.warning')}</span>
      </div>

      {errorKey && (
        <div role="alert" className="mb-4 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red">
          {t(errorKey)}
        </div>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={handleClose} disabled={isLoading}>
          {t('reset_totp.cancel')}
        </Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-status-red/30 bg-status-red-dim text-status-red hover:bg-status-red/20"
        >
          <Key className="size-4" />
          {t('reset_totp.submit')}
        </Button>
      </div>
    </Dialog>
  )
}
