import { describe, it, expect, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { createElement } from 'react'
import { useBreadcrumbs } from './useBreadcrumbs'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

function wrapper(initialPath: string) {
  return ({ children }: { children: React.ReactNode }) =>
    createElement(MemoryRouter, { initialEntries: [initialPath] }, children)
}

function crumbs(path: string) {
  const { result } = renderHook(() => useBreadcrumbs(), { wrapper: wrapper(path) })
  return result.current
}

describe('useBreadcrumbs — workspace section', () => {
  it('/workspace returns section root only', () => {
    expect(crumbs('/workspace')).toEqual([
      { label: 'breadcrumb.workspace', to: '/workspace' },
    ])
  })

  it('/workspace/dashboard returns section + page', () => {
    expect(crumbs('/workspace/dashboard')).toEqual([
      { label: 'breadcrumb.workspace', to: '/workspace' },
      { label: 'breadcrumb.dashboard' },
    ])
  })

  it('/workspace/applications returns section + list', () => {
    expect(crumbs('/workspace/applications')).toEqual([
      { label: 'breadcrumb.workspace', to: '/workspace' },
      { label: 'breadcrumb.applications', to: '/workspace/applications' },
    ])
  })

  it('/workspace/applications/:id returns section + list + detail', () => {
    const result = crumbs('/workspace/applications/app-42')
    expect(result).toEqual([
      { label: 'breadcrumb.workspace', to: '/workspace' },
      { label: 'breadcrumb.applications', to: '/workspace/applications' },
      { label: 'app-42', to: '/workspace/applications/app-42' },
    ])
  })

  it('/workspace/applications/:id/permissions returns full path', () => {
    const result = crumbs('/workspace/applications/app-42/permissions')
    expect(result).toHaveLength(4)
    expect(result[3]).toEqual({ label: 'breadcrumb.permissions' })
  })

  it('/workspace/applications/:id/routines/:routineId returns full path', () => {
    const result = crumbs('/workspace/applications/app-42/routines/r-7')
    expect(result).toHaveLength(4)
    expect(result[2].label).toBe('app-42')
    expect(result[3]).toEqual({ label: 'r-7' })
  })

  it('/workspace/executions returns section + list', () => {
    expect(crumbs('/workspace/executions')).toEqual([
      { label: 'breadcrumb.workspace', to: '/workspace' },
      { label: 'breadcrumb.executions', to: '/workspace/executions' },
    ])
  })

  it('/workspace/executions/:id returns section + list + detail', () => {
    const result = crumbs('/workspace/executions/exec-99')
    expect(result).toEqual([
      { label: 'breadcrumb.workspace', to: '/workspace' },
      { label: 'breadcrumb.executions', to: '/workspace/executions' },
      { label: 'exec-99' },
    ])
  })
})

describe('useBreadcrumbs — administration section', () => {
  it('/administration returns section root only', () => {
    expect(crumbs('/administration')).toEqual([
      { label: 'breadcrumb.administration', to: '/administration' },
    ])
  })

  it('/administration/users', () => {
    expect(crumbs('/administration/users')).toEqual([
      { label: 'breadcrumb.administration', to: '/administration' },
      { label: 'breadcrumb.admin_users' },
    ])
  })

  it('/administration/agents', () => {
    const result = crumbs('/administration/agents')
    expect(result[1]).toEqual({ label: 'breadcrumb.admin_agents' })
  })

  it('/administration/tokens', () => {
    const result = crumbs('/administration/tokens')
    expect(result[1]).toEqual({ label: 'breadcrumb.admin_tokens' })
  })

  it('/administration/permissions', () => {
    const result = crumbs('/administration/permissions')
    expect(result[1]).toEqual({ label: 'breadcrumb.permissions' })
  })
})

describe('useBreadcrumbs — other routes', () => {
  it('/account returns standalone label', () => {
    expect(crumbs('/account')).toEqual([{ label: 'breadcrumb.account' }])
  })

  it('unknown path returns empty array', () => {
    expect(crumbs('/unknown/path')).toEqual([])
  })
})