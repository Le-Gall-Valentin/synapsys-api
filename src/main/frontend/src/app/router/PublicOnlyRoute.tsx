import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config/routes'

export function PublicOnlyRoute({ children }: { children: ReactNode }) {
  const isAuthenticated = useAuth((s) => s.isAuthenticated)
  return isAuthenticated ? <Navigate to={ROUTES.PROFILE} replace /> : <>{children}</>
}
