import { useState } from 'react'
import { LogOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useShallow } from 'zustand/react/shallow'
import { useAuth } from '@/features/auth'
import { Button } from '@/shared/ui'

export function ProfilePage() {
  const { user, logout } = useAuth(
    useShallow((s) => ({ user: s.user, logout: s.logout }))
  )
  const { t } = useTranslation('profile')
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState(false)

  if (!user) return null

  const initials = user.username.slice(0, 2).toUpperCase()

  async function handleLogout(): Promise<void> {
    setIsLoggingOut(true)
    setLogoutError(false)
    try {
      await logout()
    } catch {
      setLogoutError(true)
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-bg-0 px-4">
      <div className="w-full max-w-sm rounded-xl border border-border bg-bg-1 p-8">
        {/* Avatar */}
        <div className="mb-6 flex justify-center">
          <div
            className="grid size-16 place-items-center rounded-full text-xl font-bold text-bg-0"
            style={{ background: 'linear-gradient(135deg, #5eead4, #818cf8)' }}
            aria-hidden="true"
          >
            {initials}
          </div>
          <span className="sr-only">{t('avatar', { username: user.username })}</span>
        </div>

        {/* Infos */}
        <div className="mb-6 text-center">
          <p className="text-lg font-semibold text-fg-0">{user.username}</p>
          <p className="mt-1 font-mono text-xs uppercase tracking-widest text-fg-3">
            {user.role}
          </p>
        </div>

        {/* Déconnexion */}
        <Button onClick={() => void handleLogout()} isLoading={isLoggingOut} className="w-full">
          <LogOut className="size-4" />
          {t('action.logout')}
        </Button>
        {logoutError && (
          <p role="alert" className="mt-3 text-center text-sm text-red-400">
            {t('error.logout')}
          </p>
        )}
      </div>
    </main>
  )
}