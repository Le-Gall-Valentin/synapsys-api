import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/pages/login'
import { Spinner } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import { AppLayout } from '@/app/layouts'
import { ProtectedRoute } from './ProtectedRoute'
import { AdminRoute } from './AdminRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'
import { DefaultRedirect } from './DefaultRedirect'

const DashboardPage = lazy(() =>
  import('@/pages/dashboard').then((m) => ({ default: m.DashboardPage }))
)
const ApplicationsPage = lazy(() =>
  import('@/pages/applications').then((m) => ({ default: m.ApplicationsPage }))
)
const ApplicationDetailPage = lazy(() =>
  import('@/pages/application-detail').then((m) => ({ default: m.ApplicationDetailPage }))
)
const RoutinePage = lazy(() =>
  import('@/pages/routine').then((m) => ({ default: m.RoutinePage }))
)
const ExecutionsPage = lazy(() =>
  import('@/pages/executions').then((m) => ({ default: m.ExecutionsPage }))
)
const ExecutionDetailPage = lazy(() =>
  import('@/pages/execution-detail').then((m) => ({ default: m.ExecutionDetailPage }))
)
const AdminUsersPage = lazy(() =>
  import('@/pages/admin-users').then((m) => ({ default: m.AdminUsersPage }))
)
const AdminAgentsPage = lazy(() =>
  import('@/pages/admin-agents').then((m) => ({ default: m.AdminAgentsPage }))
)
const AdminTokensPage = lazy(() =>
  import('@/pages/admin-tokens').then((m) => ({ default: m.AdminTokensPage }))
)
const PermissionsPage = lazy(() =>
  import('@/pages/permissions').then((m) => ({ default: m.PermissionsPage }))
)
const AccountPage = lazy(() =>
  import('@/pages/account').then((m) => ({ default: m.AccountPage }))
)

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route
          path={ROUTES.LOGIN}
          element={
            <PublicOnlyRoute>
              <LoginPage />
            </PublicOnlyRoute>
          }
        />

        {/* Protected — shell layout wraps all authenticated pages */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          {/* Section redirects */}
          <Route path={ROUTES.WORKSPACE} element={<Navigate to={ROUTES.DASHBOARD} replace />} />
          <Route path={ROUTES.ADMINISTRATION} element={<Navigate to={ROUTES.ADMIN_USERS} replace />} />

          {/* Workspace */}
          <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
          <Route path={ROUTES.APPLICATIONS} element={<ApplicationsPage />} />
          <Route path={ROUTES.APPLICATION_DETAIL} element={<ApplicationDetailPage />} />
          <Route path={ROUTES.APPLICATION_PERMISSIONS} element={<ApplicationDetailPage />} />
          <Route path={ROUTES.ROUTINE_DETAIL} element={<RoutinePage />} />
          <Route path={ROUTES.EXECUTIONS} element={<ExecutionsPage />} />
          <Route path={ROUTES.EXECUTION_DETAIL} element={<ExecutionDetailPage />} />

          {/* Administration (admin-only) */}
          <Route element={<AdminRoute><Outlet /></AdminRoute>}>
            <Route path={ROUTES.ADMIN_USERS} element={<AdminUsersPage />} />
            <Route path={ROUTES.ADMIN_AGENTS} element={<AdminAgentsPage />} />
            <Route path={ROUTES.ADMIN_TOKENS} element={<AdminTokensPage />} />
            <Route path={ROUTES.PERMISSIONS} element={<PermissionsPage />} />
          </Route>

          <Route path={ROUTES.ACCOUNT} element={<AccountPage />} />
        </Route>

        {/* Legacy redirect */}
        <Route
          path={ROUTES.PROFILE}
          element={
            <Suspense fallback={<Spinner />}>
              <Navigate to={ROUTES.ACCOUNT} replace />
            </Suspense>
          }
        />

        {/* Catch-all */}
        <Route path="*" element={<DefaultRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}