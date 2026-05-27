import { useTranslation } from 'react-i18next'
import { LoginForm } from '@/features/auth'
import { LoginBrandPanel } from './LoginBrandPanel'

const TITLE_ID = 'login-title'

export function LoginPage() {
  const { t } = useTranslation('login')

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">

      <LoginBrandPanel />

      {/* ── Panneau droit — Formulaire ── */}
      <main className="relative flex flex-col justify-center bg-bg-0 px-8 py-12 sm:px-11">
        <div className="mx-auto w-full max-w-[380px]">
          <div className="mb-8">
            <h1 id={TITLE_ID} className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">
              {t('form.title')}
            </h1>
            <p className="text-sm text-fg-2">{t('form.subtitle')}</p>
          </div>

          <LoginForm labelId={TITLE_ID} />

          <p className="mt-7 text-center text-[13px] leading-relaxed text-fg-2">
            <span className="font-medium text-fg-1">{t('help.no_account')}</span>{' '}
            {t('help.contact_admin')}
          </p>
        </div>

        <footer className="absolute bottom-6 left-0 right-0 text-center text-[11px] text-fg-3">
          {t('footer')}
        </footer>
      </main>
    </div>
  )
}