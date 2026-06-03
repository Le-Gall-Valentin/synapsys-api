import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config'

export function AdminRoute({ children }: { children: ReactNode }) {
  const user = useAuth((s) => s.user)
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN'
  return isAdmin ? <>{children}</> : <Navigate to={ROUTES.DASHBOARD} replace />
}