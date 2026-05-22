import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useShallow } from 'zustand/react/shallow'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isInitializing, isAuthenticated } = useAuth(
    useShallow((s) => ({ isInitializing: s.isInitializing, isAuthenticated: s.isAuthenticated }))
  )
  if (isInitializing) return <Spinner />
  return isAuthenticated ? <>{children}</> : <Navigate to={ROUTES.LOGIN} replace />
}
