import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/pages/login'
import { ProfilePage } from '@/pages/profile'
import { ROUTES } from '@/shared/config'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'
import { DefaultRedirect } from './DefaultRedirect'

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
