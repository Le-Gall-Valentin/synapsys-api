import { lazy } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/pages/login'
import { ROUTES } from '@/shared/config'
import { AppLayout } from '@/app/layouts'
import { ProtectedRoute } from './ProtectedRoute'
import { AdminRoute } from './AdminRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'
import { DefaultRedirect } from './DefaultRedirect'

const DashboardPage = lazy(() => import('@/pages/dashboard'))
const ApplicationsPage = lazy(() => import('@/pages/applications'))
const ApplicationDetailPage = lazy(() => import('@/pages/application-detail'))
const RoutinePage = lazy(() => import('@/pages/routine'))
const ExecutionsPage = lazy(() => import('@/pages/executions'))
const ExecutionDetailPage = lazy(() => import('@/pages/execution-detail'))
const AdminUsersPage = lazy(() => import('@/pages/admin-users'))
const AdminAgentsPage = lazy(() => import('@/pages/admin-agents'))
const AdminTokensPage = lazy(() => import('@/pages/admin-tokens'))
const PermissionsPage = lazy(() => import('@/pages/permissions'))
const AccountPage = lazy(() => import('@/pages/account'))

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
        <Route path={ROUTES.PROFILE} element={<Navigate to={ROUTES.ACCOUNT} replace />} />

        {/* Catch-all */}
        <Route path="*" element={<DefaultRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}