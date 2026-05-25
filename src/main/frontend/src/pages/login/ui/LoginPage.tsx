import { useTranslation } from 'react-i18next'
import { LoginForm } from '@/features/auth'
import { NetworkArtwork } from './NetworkArtwork'

export function LoginPage() {
  const { t } = useTranslation('login')

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">

      {/* ── Panneau gauche — Branding ── */}
      <div
        aria-hidden="true"
        className="relative hidden flex-col overflow-hidden border-r border-border lg:flex"
        style={{
          background:
            'radial-gradient(900px 600px at 20% 20%, rgba(94,234,212,0.10), transparent 60%), radial-gradient(700px 600px at 80% 80%, rgba(129,140,248,0.08), transparent 60%), linear-gradient(145deg, #0c1014 0%, #0a0b0d 100%)',
        }}
      >
        {/* Grille décorative */}
        <div
          className="pointer-events-none absolute inset-0"
          style={{
            backgroundImage:
              'linear-gradient(rgba(94,234,212,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(94,234,212,0.04) 1px, transparent 1px)',
            backgroundSize: '32px 32px',
            maskImage:
              'radial-gradient(ellipse 80% 60% at 50% 50%, #000 30%, transparent 80%)',
            WebkitMaskImage:
              'radial-gradient(ellipse 80% 60% at 50% 50%, #000 30%, transparent 80%)',
          }}
        />

        <div className="relative mx-auto flex h-full w-full max-w-[560px] flex-col px-11 py-10">
          {/* Brand */}
          <div className="flex items-center gap-3">
            <div
              className="grid size-9 shrink-0 place-items-center rounded-xl font-mono text-base font-bold text-bg-0"
              style={{
                background: 'linear-gradient(135deg, #5eead4, #38bdf8)',
                boxShadow:
                  '0 1px 0 rgba(255,255,255,0.15) inset, 0 2px 6px rgba(94,234,212,0.18)',
              }}
            >
              S
            </div>
            <span className="text-[17px] font-semibold tracking-tight text-fg-0">
              {t('brand')}
            </span>
          </div>

          {/* Hero */}
          <div className="mt-16">
            <p
              className="mb-4 text-[34px] font-semibold leading-[1.1] tracking-tight"
              style={{
                background: 'linear-gradient(135deg, #ffffff 0%, #b6bbc5 100%)',
                WebkitBackgroundClip: 'text',
                backgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}
            >
              {t('hero.headline_1')}
              <br />
              {t('hero.headline_2')}
            </p>
            <p className="max-w-[420px] text-[15px] leading-relaxed text-fg-1">
              {t('hero.description')}
            </p>
          </div>

          {/* SVG réseau animé */}
          <div className="mx-auto my-10 w-full max-w-[360px]">
            <NetworkArtwork />
          </div>

          {/* Citation */}
          <div
            className="mt-auto grid max-w-[440px] grid-cols-[28px_1fr] gap-3 rounded-xl border border-border p-4"
            style={{ background: 'rgba(255,255,255,0.02)' }}
          >
            <span
              className="text-4xl leading-none text-accent"
              style={{ fontFamily: 'Georgia, serif', marginTop: '4px' }}
            >
              "
            </span>
            <p className="text-[13px] italic leading-relaxed text-fg-1">
              {t('quote')}
            </p>
          </div>
        </div>
      </div>

      {/* ── Panneau droit — Formulaire ── */}
      <main className="relative flex flex-col justify-center bg-bg-0 px-8 py-12 sm:px-11">
        <div className="mx-auto w-full max-w-[380px]">
          <div className="mb-8">
            <h1 className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">
              {t('form.title')}
            </h1>
            <p className="text-sm text-fg-2">{t('form.subtitle')}</p>
          </div>

          <LoginForm />

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