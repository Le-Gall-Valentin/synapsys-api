import { useMemo } from 'react'
import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ROUTES, ROUTE_SEGMENTS } from '@/shared/config'

export interface Breadcrumb {
  label: string
  to?: string
}

export function useBreadcrumbs(): Breadcrumb[] {
  const { pathname } = useLocation()
  const { t } = useTranslation('shell')

  return useMemo(() => {
    const segments = pathname.split('/').filter(Boolean)
    // segments[0] = WORKSPACE | ADMINISTRATION | ACCOUNT
    // segments[1] = DASHBOARD | APPLICATIONS | EXECUTIONS | USERS | …
    // segments[2] = :appId | :execId
    // segments[3] = PERMISSIONS | ROUTINES
    // segments[4] = :routineId

    const workspaceRoot: Breadcrumb = { label: t('breadcrumb.workspace'), to: ROUTES.WORKSPACE }
    const adminRoot: Breadcrumb = { label: t('breadcrumb.administration'), to: ROUTES.ADMINISTRATION }

    if (segments[0] === ROUTE_SEGMENTS.WORKSPACE) {
      switch (segments[1]) {
        case ROUTE_SEGMENTS.DASHBOARD:
          return [workspaceRoot, { label: t('breadcrumb.dashboard') }]

        case ROUTE_SEGMENTS.APPLICATIONS: {
          const appsCrumb: Breadcrumb = { label: t('breadcrumb.applications'), to: ROUTES.APPLICATIONS }
          if (!segments[2]) return [workspaceRoot, appsCrumb]
          const appCrumb: Breadcrumb = { label: segments[2], to: `/workspace/applications/${segments[2]}` }
          if (segments[3] === ROUTE_SEGMENTS.PERMISSIONS) {
            return [workspaceRoot, appsCrumb, appCrumb, { label: t('breadcrumb.permissions') }]
          }
          if (segments[3] === ROUTE_SEGMENTS.ROUTINES && segments[4]) {
            return [workspaceRoot, appsCrumb, appCrumb, { label: segments[4] }]
          }
          return [workspaceRoot, appsCrumb, appCrumb]
        }

        case ROUTE_SEGMENTS.EXECUTIONS: {
          const execsCrumb: Breadcrumb = { label: t('breadcrumb.executions'), to: ROUTES.EXECUTIONS }
          if (!segments[2]) return [workspaceRoot, execsCrumb]
          return [workspaceRoot, execsCrumb, { label: segments[2] }]
        }

        default:
          return [workspaceRoot]
      }
    }

    if (segments[0] === ROUTE_SEGMENTS.ADMINISTRATION) {
      const labelMap: Record<string, string> = {
        [ROUTE_SEGMENTS.USERS]: t('breadcrumb.admin_users'),
        [ROUTE_SEGMENTS.AGENTS]: t('breadcrumb.admin_agents'),
        [ROUTE_SEGMENTS.TOKENS]: t('breadcrumb.admin_tokens'),
        [ROUTE_SEGMENTS.PERMISSIONS]: t('breadcrumb.permissions'),
      }
      if (!segments[1]) return [adminRoot]
      return [adminRoot, { label: labelMap[segments[1]] ?? segments[1] }]
    }

    if (segments[0] === ROUTE_SEGMENTS.ACCOUNT) {
      return [{ label: t('breadcrumb.account') }]
    }

    return []
  }, [pathname, t])
}