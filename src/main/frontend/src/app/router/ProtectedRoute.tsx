import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useShallow } from 'zustand/react/shallow'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isInitializing, isAuthenticated } = useAuth(
    useShallow((s) => ({ isInitializing: s.isInitializing, isAuthenticated: s.user !== null }))
  )
  const { t } = useTranslation('common')
  if (isInitializing) return <Spinner label={t('loading')} />
  return isAuthenticated ? <>{children}</> : <Navigate to={ROUTES.LOGIN} replace />
}
