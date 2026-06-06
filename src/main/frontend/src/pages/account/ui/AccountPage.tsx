import { useRef, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useShallow } from 'zustand/react/shallow'
import { KeyRound, LogOut, Shield, SlidersHorizontal, User } from 'lucide-react'
import { useAuth } from '@/features/auth'
import { totpApi } from '@/features/totp'
import { Button } from '@/shared/ui'
import { usePaletteItems } from '@/shared/lib'
import { ROUTES } from '@/shared/config'
import { accountApi } from '../api/accountApi'
import { ProfileSummaryCard } from './ProfileSummaryCard'
import { ProfileEditSection } from './ProfileEditSection'
import { TwoFactorSection } from './TwoFactorSection'
import { PreferencesSection } from './PreferencesSection'
import { ChangePasswordSection } from './ChangePasswordSection'

export function AccountPage() {
  const { t } = useTranslation('account')
  const { user, logout, patchUser } = useAuth(
    useShallow(s => ({ user: s.user, logout: s.logout, patchUser: s.patchUser }))
  )
  const paletteItems = useMemo(() => {
    const group = t('title')
    return [
      { id: 'account:profile',     label: t('profile.title'),     to: `${ROUTES.ACCOUNT}#section-profile`,     icon: User,              group },
      { id: 'account:twofa',       label: t('twofa.title'),       to: `${ROUTES.ACCOUNT}#section-twofa`,       icon: Shield,            group },
      { id: 'account:preferences', label: t('preferences.title'), to: `${ROUTES.ACCOUNT}#section-preferences`, icon: SlidersHorizontal, group },
      { id: 'account:password',    label: t('password.title'),    to: `${ROUTES.ACCOUNT}#section-password`,    icon: KeyRound,          group },
    ]
  }, [t])

  usePaletteItems('account', paletteItems)

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

  if (!user) return null

  return (
    <div className="py-5 px-6 mx-auto">
      <div className="flex flex-col gap-3 mb-[18px] sm:flex-row sm:items-start sm:justify-between sm:gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-fg-0">{t('title')}</h1>
          <p className="text-sm text-fg-2 mt-1">{t('subtitle')}</p>
        </div>
        <Button
          onClick={() => { void handleLogout() }}
          isLoading={isLoggingOut}
          className="self-start shrink-0 border-status-red/30 bg-transparent text-status-red hover:bg-status-red-dim"
        >
          <LogOut className="size-4" />
          {t('action.logout')}
        </Button>
      </div>

      {logoutError && (
        <p className="mb-4 text-sm text-status-red" role="alert">{logoutError}</p>
      )}

      <ProfileSummaryCard user={user} />

      <ProfileEditSection
        user={user}
        onPatch={patchUser}
        onUpdateProfile={accountApi.updateProfile}
      />

      <TwoFactorSection
        user={user}
        onPatch={patchUser}
        enrollApi={totpApi}
      />

      <PreferencesSection />

      <ChangePasswordSection
        onChangePassword={accountApi.changePassword}
      />
    </div>
  )
}