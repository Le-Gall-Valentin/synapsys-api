import { useRef, useState } from 'react'
import { LogOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useShallow } from 'zustand/react/shallow'
import { useAuth } from '@/features/auth'
import { Button } from '@/shared/ui'

const AVATAR_GRADIENT = 'linear-gradient(135deg, var(--brand-icon-from), var(--brand-icon-to))'

export function ProfilePage() {
  const { user, logout } = useAuth(
    useShallow((s) => ({ user: s.user, logout: s.logout }))
  )
  const { t } = useTranslation('profile')
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const isLoggingOutRef = useRef(false)

  if (!user) return null

  const initials = [...user.username.trim()].slice(0, 2).join('').toUpperCase() || '?'

  async function handleLogout(): Promise<void> {
    if (isLoggingOutRef.current) return
    isLoggingOutRef.current = true
    setIsLoggingOut(true)
    setLogoutError(null)
    try {
      await logout()
    } catch {
      setLogoutError(t('error.logout_failed'))
    } finally {
      isLoggingOutRef.current = false
      setIsLoggingOut(false)
    }
  }

  return (
    <main className="flex min-h-screen flex-col bg-bg-0 px-4">
      <header className="flex items-center gap-2.5 py-5">
        <div
          className="grid size-8 shrink-0 place-items-center rounded-lg font-mono text-sm font-bold text-bg-0"
          style={{ background: AVATAR_GRADIENT }}
        >
          S
        </div>
        <span className="text-[15px] font-semibold tracking-tight text-fg-0">SynapSys</span>
      </header>

      <div className="flex flex-1 items-center justify-center">
      <div className="w-full max-w-sm rounded-xl border border-border bg-bg-1 p-8">
        {/* Avatar */}
        <div className="mb-6 flex justify-center">
          <div
            role="img"
            aria-label={t('avatar', { username: user.username })}
            className="grid size-16 place-items-center rounded-full text-xl font-bold text-bg-0"
            style={{ background: AVATAR_GRADIENT }}
          >
            {initials}
          </div>
        </div>

        {/* Infos */}
        <div className="mb-6 text-center">
          <p className="text-lg font-semibold text-fg-0">{user.username}</p>
          <p className="mt-1 font-mono text-xs uppercase tracking-widest text-fg-3">
            {user.role}
          </p>
        </div>

        {logoutError && (
          <p className="mb-3 text-center text-sm text-status-red" role="alert">
            {logoutError}
          </p>
        )}

        <Button onClick={() => { void handleLogout() }} isLoading={isLoggingOut} className="w-full">
          <LogOut className="size-4" />
          {t('action.logout')}
        </Button>
      </div>
      </div>
    </main>
  )
}