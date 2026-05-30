import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { LoginForm, useAuth } from '@/features/auth'
import type { LoginOutcome } from '@/features/auth'
import { TotpVerifyStep, TotpEnrollProposal, TotpSetupFlow, totpApi } from '@/features/totp'
import type { User } from '@/entities/user'
import { LoginBrandPanel } from './LoginBrandPanel'

type LoginStep = 'credentials' | 'totp' | 'enroll' | 'setup'

const TITLE_ID = 'login-title'

export function LoginPage() {
  const { t } = useTranslation('login')
  const finalizeLogin = useAuth(s => s.finalizeLogin)

  const [step, setStep] = useState<LoginStep>('credentials')
  const [pendingUser, setPendingUser] = useState<User | null>(null)
  const [pendingUsername, setPendingUsername] = useState('')

  function handleLoginOutcome(outcome: Exclude<LoginOutcome, { kind: 'authenticated' }>) {
    if (outcome.kind === 'totp_required') {
      setPendingUsername(outcome.username)
      setStep('totp')
    } else {
      // enrollment_proposed
      setPendingUser(outcome.user)
      setStep('enroll')
    }
  }

  function handleVerified(user: User) {
    finalizeLogin(user)
  }

  function handleBack() {
    setStep('credentials')
    setPendingUser(null)
    setPendingUsername('')
  }

  function handleActivate() {
    setStep('setup')
  }

  function handleSkip() {
    if (pendingUser) finalizeLogin(pendingUser)
  }

  function handleSetupSuccess() {
    if (pendingUser) finalizeLogin({ ...pendingUser, totpEnabled: true })
  }

  function handleSetupDismiss() {
    if (pendingUser) finalizeLogin(pendingUser)
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      <LoginBrandPanel />

      <main className="relative flex flex-col justify-center bg-bg-0 px-8 py-12 sm:px-11">
        <div className="mx-auto w-full max-w-95">

          {step === 'credentials' && (
            <>
              <div className="mb-8">
                <h1 id={TITLE_ID} className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">
                  {t('form.title')}
                </h1>
                <p className="text-sm text-fg-2">{t('form.subtitle')}</p>
              </div>
              <LoginForm labelId={TITLE_ID} onLoginOutcome={handleLoginOutcome} />
              <p className="mt-7 text-center text-[13px] leading-relaxed text-fg-2">
                <span className="font-medium text-fg-1">{t('help.no_account')}</span>{' '}
                {t('help.contact_admin')}
              </p>
            </>
          )}

          {step === 'totp' && (
            <TotpVerifyStep
              username={pendingUsername}
              api={totpApi}
              onVerified={handleVerified}
              onBack={handleBack}
            />
          )}

          {step === 'enroll' && pendingUser && (
            <TotpEnrollProposal
              username={pendingUser.username}
              onActivate={handleActivate}
              onSkip={handleSkip}
            />
          )}

          {step === 'setup' && (
            <TotpSetupFlow
              api={totpApi}
              onSuccess={handleSetupSuccess}
              onDismiss={handleSetupDismiss}
            />
          )}

        </div>

        <footer className="absolute bottom-6 left-0 right-0 text-center text-[11px] text-fg-3">
          {t('footer')}
        </footer>
      </main>
    </div>
  )
}