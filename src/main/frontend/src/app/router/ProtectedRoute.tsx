import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const isInitializing = useAuth((s) => s.isInitializing)
  const isAuthenticated = useAuth((s) => s.isAuthenticated)
  if (isInitializing) return <Spinner />
  return isAuthenticated ? <>{children}</> : <Navigate to={ROUTES.LOGIN} replace />
}
