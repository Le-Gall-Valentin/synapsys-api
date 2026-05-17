import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { LoginPage } from '@/pages/login'
import { ProfilePage } from '@/pages/profile'
import { ROUTES } from '@/shared/config/routes'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'

function DefaultRedirect() {
  const isAuthenticated = useAuth((s) => s.isAuthenticated)
  return <Navigate to={isAuthenticated ? ROUTES.PROFILE : ROUTES.LOGIN} replace />
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path={ROUTES.LOGIN}
          element={
            <PublicOnlyRoute>
              <LoginPage />
            </PublicOnlyRoute>
          }
        />
        <Route
          path={ROUTES.PROFILE}
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<DefaultRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}
