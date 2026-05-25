import { Navigate } from 'react-router-dom'
import { useShallow } from 'zustand/react/shallow'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'

export function DefaultRedirect() {
  const { isInitializing, isAuthenticated } = useAuth(
    useShallow((s) => ({ isInitializing: s.isInitializing, isAuthenticated: s.user !== null }))
  )
  const { t } = useTranslation('common')
  if (isInitializing) return <Spinner label={t('loading')} />
  return <Navigate to={isAuthenticated ? ROUTES.PROFILE : ROUTES.LOGIN} replace />
}