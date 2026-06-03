import { useMemo } from 'react'
import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ROUTES } from '@/shared/config'

export interface Breadcrumb {
  label: string
  to?: string
}

export function useBreadcrumbs(): Breadcrumb[] {
  const { pathname } = useLocation()
  const { t } = useTranslation('shell')

  return useMemo(() => {
    const segments = pathname.split('/').filter(Boolean)
    // segments[0] = 'workspace' | 'administration' | 'account'
    // segments[1] = 'dashboard' | 'applications' | 'executions' | 'users' | ...
    // segments[2] = :appId | :execId
    // segments[3] = 'permissions' | 'routines'
    // segments[4] = :routineId

    const workspaceRoot: Breadcrumb = { label: t('breadcrumb.workspace'), to: ROUTES.WORKSPACE }
    const adminRoot: Breadcrumb = { label: t('breadcrumb.administration'), to: ROUTES.ADMINISTRATION }

    if (segments[0] === 'workspace') {
      switch (segments[1]) {
        case 'dashboard':
          return [workspaceRoot, { label: t('breadcrumb.dashboard') }]

        case 'applications': {
          const appsCrumb: Breadcrumb = { label: t('breadcrumb.applications'), to: ROUTES.APPLICATIONS }
          if (!segments[2]) return [workspaceRoot, appsCrumb]
          const appCrumb: Breadcrumb = { label: segments[2], to: `/workspace/applications/${segments[2]}` }
          if (segments[3] === 'permissions') {
            return [workspaceRoot, appsCrumb, appCrumb, { label: t('breadcrumb.permissions') }]
          }
          if (segments[3] === 'routines' && segments[4]) {
            return [workspaceRoot, appsCrumb, appCrumb, { label: segments[4] }]
          }
          return [workspaceRoot, appsCrumb, appCrumb]
        }

        case 'executions': {
          const execsCrumb: Breadcrumb = { label: t('breadcrumb.executions'), to: ROUTES.EXECUTIONS }
          if (!segments[2]) return [workspaceRoot, execsCrumb]
          return [workspaceRoot, execsCrumb, { label: segments[2] }]
        }

        default:
          return [workspaceRoot]
      }
    }

    if (segments[0] === 'administration') {
      const labelMap: Record<string, string> = {
        users: t('breadcrumb.admin_users'),
        agents: t('breadcrumb.admin_agents'),
        tokens: t('breadcrumb.admin_tokens'),
        permissions: t('breadcrumb.permissions'),
      }
      if (!segments[1]) return [adminRoot]
      return [adminRoot, { label: labelMap[segments[1]] ?? segments[1] }]
    }

    if (segments[0] === 'account') {
      return [{ label: t('breadcrumb.account') }]
    }

    return []
  }, [pathname, t])
}