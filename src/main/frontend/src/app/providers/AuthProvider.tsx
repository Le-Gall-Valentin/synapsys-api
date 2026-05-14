import { useEffect, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { registerLogoutCallback } from '@/shared/lib/authCallbacks'
import { ROUTES } from '@/shared/config/routes'
import { Spinner } from '@/shared/ui'

export function AuthProvider({ children }: { children: ReactNode }) {
  const { initialize, isInitializing, isAuthenticated } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    registerLogoutCallback(() => {
      void useAuth.getState().logout()
    })

    void initialize()
  }, [initialize])

  useEffect(() => {
    if (!isAuthenticated && !isInitializing) {
      navigate(ROUTES.LOGIN, { replace: true })
    }
  }, [isAuthenticated, isInitializing, navigate])

  if (isInitializing) return <Spinner />
  return <>{children}</>
}