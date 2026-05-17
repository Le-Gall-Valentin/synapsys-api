import { useEffect, type ReactNode } from 'react'
import { useAuth } from '@/features/auth'
import { registerLogoutCallback } from '@/shared/lib/authCallbacks'
import { Spinner } from '@/shared/ui'

export function AuthProvider({ children }: { children: ReactNode }) {
  const { initialize, isInitializing, logout } = useAuth()

  useEffect(() => {
    registerLogoutCallback(() => void logout())
    void initialize()
  }, [initialize, logout])

  if (isInitializing) return <Spinner />
  return <>{children}</>
}