import { LogOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config/routes'
import { Button } from '@/shared/ui'

export function ProfilePage() {
  const user = useAuth((s) => s.user)
  const logout = useAuth((s) => s.logout)
  const { t } = useTranslation('profile')

  if (!user) return <Navigate to={ROUTES.LOGIN} replace />

  const initials = user.username.slice(0, 2).toUpperCase()

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-bg-0 px-4">
      <div className="w-full max-w-sm rounded-xl border border-border bg-bg-1 p-8">
        {/* Avatar */}
        <div className="mb-6 flex justify-center">
          <div
            className="grid size-16 place-items-center rounded-full text-xl font-bold text-bg-0"
            style={{ background: 'linear-gradient(135deg, #5eead4, #818cf8)' }}
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

        {/* Déconnexion */}
        <Button onClick={() => void logout()} className="w-full">
          <LogOut className="size-4" />
          {t('action.logout')}
        </Button>
      </div>
    </div>
  )
}
