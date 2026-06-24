import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { Alert, Pagination, SearchInput, CTA_BUTTON_STYLE } from '@/shared/ui'
import { ROUTES } from '@/shared/config/routes'
import { agentsApi } from '../api/agentsApi'
import type { IAgentsApi } from '../model/IAgentsApi'
import { AdminAgentsApiProvider } from '../model/agentsApiContext'
import { useAgentsPage } from '../model/useAgentsPage'
import { AgentStatsCards } from './AgentStatsCards'
import { AgentsTable } from './AgentsTable'
import { AgentsCardList } from './AgentsCardList'
import { ConfirmAgentActionModal } from './ConfirmAgentActionModal'

interface AdminAgentsPageProps {
  /** Composition seam : implémentation réelle par défaut ; les tests injectent un faux. */
  api?: IAgentsApi
}

/** Racine de composition de la slice : fournit l'API agents à sa propre frontière. */
export function AdminAgentsPage({ api = agentsApi }: AdminAgentsPageProps = {}) {
  return (
    <AdminAgentsApiProvider api={api}>
      <AdminAgentsPageContent />
    </AdminAgentsApiProvider>
  )
}

function AdminAgentsPageContent() {
  const { t } = useTranslation('adminAgents')
  const vm = useAgentsPage()

  const showPagination = !vm.isPending && vm.totalPages > 1

  return (
    <div className="py-5 px-6 mx-auto">
      <div className="flex flex-col gap-3 mb-4 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-fg-0">{t('title')}</h1>
          <p className="text-sm text-fg-2 mt-1">{t('subtitle')}</p>
        </div>
        <Link
          to={ROUTES.ADMIN_TOKENS}
          className="flex items-center justify-center gap-1.5 self-start shrink-0 rounded-lg border border-transparent px-4 py-2.5 text-sm font-semibold transition-colors"
          style={CTA_BUTTON_STYLE}
        >
          <Plus className="size-4" />
          {t('action.add_server')}
        </Link>
      </div>

      <AgentStatsCards stats={vm.stats} isLoading={vm.statsPending} isError={vm.statsError} />

      {vm.loadError && <Alert variant="error" className="mb-4">{t('load_error')}</Alert>}

      <SearchInput
        value={vm.searchInput}
        onChange={vm.changeSearch}
        placeholder={t('search.placeholder')}
        clearLabel={t('search.clear')}
        className="mb-3 max-w-md"
      />

      <div className="hidden md:block">
        <AgentsTable
          agents={vm.agents}
          isLoading={vm.isPending}
          sortBy={vm.sortBy}
          sortDirection={vm.sortDirection}
          onSort={vm.toggleSort}
          pendingActionId={vm.pendingActionId}
          onRevoke={vm.setRevokeTarget}
          onDelete={vm.setDeleteTarget}
        />
      </div>
      <div className="md:hidden">
        <AgentsCardList
          agents={vm.agents}
          isLoading={vm.isPending}
          pendingActionId={vm.pendingActionId}
          onRevoke={vm.setRevokeTarget}
          onDelete={vm.setDeleteTarget}
        />
      </div>

      {showPagination && (
        <div className="mt-3">
          <Pagination
            page={vm.page}
            totalPages={vm.totalPages}
            onPageChange={vm.setPage}
            pageLabel={t('pagination.page', { current: vm.page + 1, total: vm.totalPages })}
            prevLabel={t('pagination.prev')}
            nextLabel={t('pagination.next')}
            summary={t('pagination.total', { count: vm.totalElements })}
            isTransitioning={vm.isPlaceholderData}
          />
        </div>
      )}

      {vm.revokeTarget && (
        <ConfirmAgentActionModal
          agent={vm.revokeTarget}
          action="revoke"
          onClose={() => vm.setRevokeTarget(null)}
          onConfirm={vm.revokeAgent.mutateAsync}
          onSuccess={vm.onRevokeSuccess}
        />
      )}

      {vm.deleteTarget && (
        <ConfirmAgentActionModal
          agent={vm.deleteTarget}
          action="delete"
          onClose={() => vm.setDeleteTarget(null)}
          onConfirm={vm.deleteAgent.mutateAsync}
          onSuccess={vm.onDeleteSuccess}
        />
      )}
    </div>
  )
}