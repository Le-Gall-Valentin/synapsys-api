import { useState, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Input } from '@/shared/ui'
import { InvalidCurrentPasswordError } from '../api/accountApi'
import { NetworkError } from '@/shared/lib'

type Flash = { kind: 'success' | 'error'; key: string } | null

interface ChangePasswordSectionProps {
  onChangePassword: (currentPassword: string, newPassword: string) => Promise<void>
}

export function ChangePasswordSection({ onChangePassword }: ChangePasswordSectionProps) {
  const { t } = useTranslation('account')
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [flash, setFlash] = useState<Flash>(null)
  const flashTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$/
  const mismatch = next.length > 0 && confirm.length > 0 && next !== confirm
  const tooShort = next.length > 0 && next.length < 8
  const weak = next.length >= 8 && next.length <= 72 && !PASSWORD_REGEX.test(next)
  const canSubmit =
    current.length > 0 &&
    next.length >= 8 &&
    next.length <= 72 &&
    PASSWORD_REGEX.test(next) &&
    next === confirm

  function showFlash(kind: 'success' | 'error', key: string) {
    if (flashTimer.current) clearTimeout(flashTimer.current)
    setFlash({ kind, key })
    flashTimer.current = setTimeout(() => setFlash(null), 3000)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || isSubmitting) return
    setIsSubmitting(true)
    try {
      await onChangePassword(current, next)
      setCurrent('')
      setNext('')
      setConfirm('')
      showFlash('success', 'password.success')
    } catch (error) {
      if (error instanceof InvalidCurrentPasswordError) {
        showFlash('error', 'password.error.wrong_current')
      } else if (error instanceof NetworkError) {
        showFlash('error', 'password.error.network')
      } else {
        showFlash('error', 'password.error.server')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="rounded-xl border border-border bg-bg-1 p-4 mb-4">
      <div className="mb-4">
        <div className="font-medium text-fg-0 text-sm">{t('password.title')}</div>
        <div className="text-xs text-fg-2 mt-0.5">{t('password.subtitle')}</div>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="mb-3 max-w-[360px]">
          <Input
            label={t('password.current')}
            name="currentPassword"
            type="password"
            value={current}
            onChange={e => setCurrent(e.target.value)}
            autoComplete="current-password"
            disabled={isSubmitting}
          />
        </div>
        <div className="flex gap-3 flex-wrap sm:flex-nowrap mb-1">
          <div className="flex-1 min-w-0">
            <Input
              label={t('password.new')}
              name="newPassword"
              type="password"
              value={next}
              onChange={e => setNext(e.target.value)}
              autoComplete="new-password"
              disabled={isSubmitting}
            />
          </div>
          <div className="flex-1 min-w-0">
            <Input
              label={t('password.confirm')}
              name="confirmPassword"
              type="password"
              value={confirm}
              onChange={e => setConfirm(e.target.value)}
              autoComplete="new-password"
              disabled={isSubmitting}
            />
          </div>
        </div>
        {mismatch && <p className="text-xs text-status-red mb-2">{t('password.error.mismatch')}</p>}
        {!mismatch && tooShort && <p className="text-xs text-status-orange mb-2">{t('password.error.too_short')}</p>}
        {!mismatch && !tooShort && weak && <p className="text-xs text-status-orange mb-2">{t('password.error.weak')}</p>}
        {flash && (
          <div
            role={flash.kind === 'success' ? 'status' : 'alert'}
            className={`mb-3 text-xs px-3 py-2 rounded-lg ${flash.kind === 'success' ? 'bg-status-green-dim text-status-green' : 'bg-status-red-dim text-status-red'}`}
          >
            {t(flash.key)}
          </div>
        )}
        <div className="flex justify-end mt-2">
          <Button type="submit" disabled={!canSubmit} isLoading={isSubmitting}>
            {t('password.submit')}
          </Button>
        </div>
      </form>
    </section>
  )
}