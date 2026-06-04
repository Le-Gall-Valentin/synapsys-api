import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useShallow } from 'zustand/react/shallow'
import { LogOut } from 'lucide-react'
import { useAuth } from '@/features/auth'
import { Button } from '@/shared/ui'

export function AccountPage() {
  const { t } = useTranslation('account')
  const { logout } = useAuth(useShallow((s) => ({ logout: s.logout })))
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const pendingRef = useRef(false)

  async function handleLogout(): Promise<void> {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoggingOut(true)
    setLogoutError(null)
    try {
      await logout()
    } catch {
      setLogoutError(t('error.logout_failed'))
    } finally {
      pendingRef.current = false
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="p-6">
      <h1 className="mb-6 text-2xl font-semibold tracking-tight text-fg-0">{t('title')}</h1>
      {logoutError && (
        <p className="mb-3 text-sm text-status-red" role="alert">{logoutError}</p>
      )}
      <Button onClick={() => { void handleLogout() }} isLoading={isLoggingOut}>
        <LogOut className="size-4" />
        {t('action.logout')}
      </Button>
    </div>
  )
}