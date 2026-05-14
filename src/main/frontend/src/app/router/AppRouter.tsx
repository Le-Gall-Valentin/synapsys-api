import { useEffect, type ReactNode } from 'react'
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { LoginPage } from '@/pages/login'
import { ProfilePage } from '@/pages/profile'
import { Spinner } from '@/shared/ui'
import { ROUTES } from '@/shared/config/routes'

function AuthInitializer({ children }: { children: ReactNode }) {
  const { initialize, isInitializing } = useAuth()

  useEffect(() => {
    void initialize()
  }, [initialize])

  if (isInitializing) return <Spinner />
  return <>{children}</>
}

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? (
    <>{children}</>
  ) : (
    <Navigate to={ROUTES.LOGIN} replace />
  )
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <AuthInitializer>
        <Routes>
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />
          <Route
            path={ROUTES.PROFILE}
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to={ROUTES.PROFILE} replace />} />
        </Routes>
      </AuthInitializer>
    </BrowserRouter>
  )
}