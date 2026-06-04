import type { LucideIcon } from 'lucide-react'
import { Activity, Grid2x2, KeyRound, LayoutDashboard, Server, Shield, Users } from 'lucide-react'
import { ROUTES } from '@/shared/config'

export interface NavItemConfig {
  id: string
  to: string
  icon: LucideIcon
  labelKey: string
  adminOnly?: boolean
  sectionKey: string
}

export const NAV_CONFIG: NavItemConfig[] = [
  { id: 'shell:dashboard', sectionKey: 'nav.section.workspace', to: ROUTES.DASHBOARD, icon: LayoutDashboard, labelKey: 'nav.dashboard' },
  { id: 'shell:applications', sectionKey: 'nav.section.workspace', to: ROUTES.APPLICATIONS, icon: Grid2x2, labelKey: 'nav.applications' },
  { id: 'shell:executions', sectionKey: 'nav.section.workspace', to: ROUTES.EXECUTIONS, icon: Activity, labelKey: 'nav.executions' },
  { id: 'shell:users', sectionKey: 'nav.section.admin', adminOnly: true, to: ROUTES.ADMIN_USERS, icon: Users, labelKey: 'nav.users' },
  { id: 'shell:agents', sectionKey: 'nav.section.admin', adminOnly: true, to: ROUTES.ADMIN_AGENTS, icon: Server, labelKey: 'nav.agents' },
  { id: 'shell:tokens', sectionKey: 'nav.section.admin', adminOnly: true, to: ROUTES.ADMIN_TOKENS, icon: KeyRound, labelKey: 'nav.tokens' },
  { id: 'shell:permissions', sectionKey: 'nav.section.admin', adminOnly: true, to: ROUTES.PERMISSIONS, icon: Shield, labelKey: 'nav.permissions' },
]