export const ROUTES = {
  LOGIN: '/login',

  // Section redirects (auto-redirect to first item)
  WORKSPACE: '/workspace',
  ADMINISTRATION: '/administration',

  // Workspace
  DASHBOARD: '/workspace/dashboard',
  APPLICATIONS: '/workspace/applications',
  APPLICATION_DETAIL: '/workspace/applications/:appId',
  APPLICATION_PERMISSIONS: '/workspace/applications/:appId/permissions',
  ROUTINE_DETAIL: '/workspace/applications/:appId/routines/:routineId',
  EXECUTIONS: '/workspace/executions',
  EXECUTION_DETAIL: '/workspace/executions/:execId',

  // Administration
  ADMIN_USERS: '/administration/users',
  ADMIN_AGENTS: '/administration/agents',
  ADMIN_TOKENS: '/administration/tokens',
  PERMISSIONS: '/administration/permissions',

  // Account
  ACCOUNT: '/account',

  // Legacy redirect
  PROFILE: '/profile',
} as const

export const ROUTE_SEGMENTS = {
  WORKSPACE: 'workspace',
  ADMINISTRATION: 'administration',
  ACCOUNT: 'account',
  DASHBOARD: 'dashboard',
  APPLICATIONS: 'applications',
  EXECUTIONS: 'executions',
  PERMISSIONS: 'permissions',
  ROUTINES: 'routines',
  USERS: 'users',
  AGENTS: 'agents',
  TOKENS: 'tokens',
} as const

export const toApplicationDetail = (appId: string) =>
  `/workspace/applications/${appId}`

export const toApplicationPermissions = (appId: string) =>
  `/workspace/applications/${appId}/permissions`

export const toRoutineDetail = (appId: string, routineId: string) =>
  `/workspace/applications/${appId}/routines/${routineId}`

export const toExecutionDetail = (execId: string) =>
  `/workspace/executions/${execId}`