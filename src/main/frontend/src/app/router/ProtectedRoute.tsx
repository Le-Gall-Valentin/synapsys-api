import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config/routes'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to={ROUTES.LOGIN} replace />
}