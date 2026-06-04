import { useState, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Shield } from 'lucide-react'
import { Button, Dialog } from '@/shared/ui'
import { TotpSetupFlow } from '@/features/totp'
import type { ITotpEnrollApi } from '@/features/totp'
import type { User } from '@/entities/user'
import { DisableTotpModal } from './DisableTotpModal'

type Flash = { kind: 'success' | 'error'; key: string } | null

interface TwoFactorSectionProps {
  user: User
  onPatch: (partial: Partial<User>) => void
  enrollApi: ITotpEnrollApi
}

export function TwoFactorSection({ user, onPatch, enrollApi }: TwoFactorSectionProps) {
  const { t } = useTranslation('account')
  const [enableOpen, setEnableOpen] = useState(false)
  const [disableOpen, setDisableOpen] = useState(false)
  const [flash, setFlash] = useState<Flash>(null)
  const flashTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  function showFlash(kind: 'success' | 'error', key: string) {
    if (flashTimer.current) clearTimeout(flashTimer.current)
    setFlash({ kind, key })
    flashTimer.current = setTimeout(() => setFlash(null), 3000)
  }

  function handleEnableSuccess() {
    onPatch({ totpEnabled: true })
    setEnableOpen(false)
    showFlash('success', 'twofa.success_enabled')
  }

  function handleDisableSuccess() {
    onPatch({ totpEnabled: false })
    setDisableOpen(false)
    showFlash('success', 'twofa.success_disabled')
  }

  return (
    <section className="rounded-xl border border-border bg-bg-1 p-4 mb-4">
      <div className="flex items-start justify-between gap-4 mb-3">
        <div>
          <div className="font-medium text-fg-0 text-sm">{t('twofa.title')}</div>
          <div className="text-xs text-fg-2 mt-0.5">{t('twofa.subtitle')}</div>
        </div>
        <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium border ${user.totpEnabled ? 'border-status-green/30 bg-status-green-dim text-status-green' : 'border-status-orange/30 bg-status-orange-dim text-status-orange'}`}>
          {user.totpEnabled ? t('twofa.status_enabled') : t('twofa.status_disabled')}
        </span>
      </div>
      <div className="flex items-start gap-3">
        <div className={`mt-0.5 rounded-lg p-2 ${user.totpEnabled ? 'bg-accent-dim text-accent' : 'bg-bg-3 text-fg-2'}`}>
          <Shield className="size-5" />
        </div>
        <p className="flex-1 text-xs text-fg-1 leading-relaxed">
          {user.totpEnabled ? t('twofa.desc_enabled') : t('twofa.desc_disabled')}
        </p>
        {user.totpEnabled ? (
          <Button
            onClick={() => setDisableOpen(true)}
            className="shrink-0 border-status-red/30 bg-status-red-dim text-status-red hover:bg-status-red/20"
          >
            {t('twofa.btn_disable')}
          </Button>
        ) : (
          <Button onClick={() => setEnableOpen(true)} className="shrink-0">
            <Shield className="size-3.5" />
            {t('twofa.btn_enable')}
          </Button>
        )}
      </div>
      {flash && (
        <div
          role={flash.kind === 'success' ? 'status' : 'alert'}
          className={`mt-3 text-xs px-3 py-2 rounded-lg ${flash.kind === 'success' ? 'bg-status-green-dim text-status-green' : 'bg-status-red-dim text-status-red'}`}
        >
          {t(flash.key)}
        </div>
      )}

      <Dialog
        open={enableOpen}
        onClose={() => setEnableOpen(false)}
        title={t('twofa.btn_enable')}
        maxWidth="max-w-lg"
      >
        <TotpSetupFlow
          api={enrollApi}
          onSuccess={handleEnableSuccess}
          onDismiss={() => setEnableOpen(false)}
          dismissLabel={t('setup.dismiss_profile', { ns: 'totp' })}
        />
      </Dialog>

      <DisableTotpModal
        open={disableOpen}
        onClose={() => setDisableOpen(false)}
        onSuccess={handleDisableSuccess}
        onDisable={enrollApi.disable}
      />
    </section>
  )
}