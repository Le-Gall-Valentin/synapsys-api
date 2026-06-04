import { Suspense, useCallback, useEffect, useMemo, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Activity,
  Grid2x2,
  KeyRound,
  LayoutDashboard,
  Server,
  Shield,
  User,
  Users,
} from 'lucide-react'
import { useAuth } from '@/features/auth'
import { usePaletteItems } from '@/shared/lib'
import { ROUTES } from '@/shared/config'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { CommandPalette } from './CommandPalette'

function PageLoader() {
  const { t } = useTranslation('shell')
  return (
    <div className="flex h-32 items-center justify-center gap-3 text-xs text-fg-2">
      <div className="size-4 animate-spin rounded-full border-2 border-border-2 border-t-accent" />
      {t('loader')}
    </div>
  )
}

function ShellPaletteRegistrar() {
  const { t } = useTranslation('shell')
  const user = useAuth((s) => s.user)
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN'

  const pageGroup = t('palette.type_page')

  const items = useMemo(
    () =>
      [
        { id: 'shell:dashboard', label: t('nav.dashboard'), to: ROUTES.DASHBOARD, icon: LayoutDashboard, group: pageGroup },
        { id: 'shell:applications', label: t('nav.applications'), to: ROUTES.APPLICATIONS, icon: Grid2x2, group: pageGroup },
        { id: 'shell:executions', label: t('nav.executions'), to: ROUTES.EXECUTIONS, icon: Activity, group: pageGroup },
        ...(isAdmin
          ? [
              { id: 'shell:users', label: t('nav.users'), to: ROUTES.ADMIN_USERS, icon: Users, group: pageGroup },
              { id: 'shell:agents', label: t('nav.agents'), to: ROUTES.ADMIN_AGENTS, icon: Server, group: pageGroup },
              { id: 'shell:tokens', label: t('nav.tokens'), to: ROUTES.ADMIN_TOKENS, icon: KeyRound, group: pageGroup },
              { id: 'shell:permissions', label: t('nav.permissions'), to: ROUTES.PERMISSIONS, icon: Shield, group: pageGroup },
            ]
          : []),
        { id: 'shell:account', label: t('nav.account'), to: ROUTES.ACCOUNT, icon: User, group: pageGroup },
      ],
    [t, isAdmin, pageGroup]
  )

  usePaletteItems('shell', items)
  return null
}

export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [paletteOpen, setPaletteOpen] = useState(false)

  const openSidebar = useCallback(() => setSidebarOpen(true), [])
  const closeSidebar = useCallback(() => setSidebarOpen(false), [])
  const openPalette = useCallback(() => setPaletteOpen(true), [])
  const closePalette = useCallback(() => setPaletteOpen(false), [])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        setPaletteOpen(true)
      } else if (
        e.key === '/' &&
        document.activeElement?.tagName !== 'INPUT' &&
        document.activeElement?.tagName !== 'TEXTAREA' &&
        !(document.activeElement instanceof HTMLElement && document.activeElement.isContentEditable)
      ) {
        e.preventDefault()
        setPaletteOpen(true)
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [])

  return (
    <div className="flex h-screen overflow-hidden bg-bg-0">
      <ShellPaletteRegistrar />

      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          aria-hidden="true"
          onClick={closeSidebar}
        />
      )}

      <Sidebar open={sidebarOpen} onClose={closeSidebar} />

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Topbar onMenuOpen={openSidebar} onSearchOpen={openPalette} />
        <main className="flex-1 overflow-y-auto">
          <Suspense fallback={<PageLoader />}>
            <Outlet />
          </Suspense>
        </main>
      </div>

      {paletteOpen && <CommandPalette onClose={closePalette} />}
    </div>
  )
}