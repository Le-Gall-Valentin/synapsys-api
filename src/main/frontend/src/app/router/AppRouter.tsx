import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/pages/login'
import { ProfilePage } from '@/pages/profile'
import { ROUTES } from '@/shared/config/routes'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'

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
        <Route path="*" element={<Navigate to={ROUTES.PROFILE} replace />} />
      </Routes>
    </BrowserRouter>
  )
}