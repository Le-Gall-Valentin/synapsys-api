import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { Alert, Pagination, SearchInput, CTA_BUTTON_STYLE } from '@/shared/ui'
import { useDebouncedValue, pageAfterRemoval } from '@/shared/lib'
import { ROUTES } from '@/shared/config/routes'
import { agentsApi } from '../api/agentsApi'
import type { IAgentsApi, Agent, AgentSortField, SortDirection } from '../model/IAgentsApi'
import { AdminAgentsApiProvider } from '../model/agentsApiContext'
import { useAgents, AGENTS_PAGE_SIZE } from '../model/useAgents'
import { useAgentStatistics } from '../model/useAgentStatistics'
import { useRevokeAgent, useDeleteAgent } from '../model/useAgentMutations'
import { AgentStatsCards } from './AgentStatsCards'
import { AgentsTable } from './AgentsTable'
import { AgentsCardList } from './AgentsCardList'
import { RevokeAgentModal } from './RevokeAgentModal'
import { DeleteAgentModal } from './DeleteAgentModal'

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

  const [page, setPage] = useState(0)
  const [sortBy, setSortBy] = useState<AgentSortField>('enrolledAt')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [searchInput, setSearchInput] = useState('')
  const search = useDebouncedValue(searchInput, 300)

  const { data, isPending, isError: loadError, isPlaceholderData } = useAgents(page, sortBy, sortDirection, search)
  const { data: stats, isPending: statsPending } = useAgentStatistics()

  const [revokeTarget, setRevokeTarget] = useState<Agent | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Agent | null>(null)

  const revokeAgent = useRevokeAgent()
  const deleteAgent = useDeleteAgent()

  const agents = data?.content ?? []
  const totalElements = data?.totalElements ?? 0
  const pageSize = data?.size ?? AGENTS_PAGE_SIZE
  const totalPages = totalElements > 0 ? Math.ceil(totalElements / pageSize) : 1
  const showPagination = !isPending && totalPages > 1
  const pendingActionId = revokeAgent.isPending ? revokeAgent.variables
    : deleteAgent.isPending ? deleteAgent.variables : null

  function handleSearchChange(value: string) {
    setSearchInput(value)
    setPage(0)
  }

  function handleSort(field: AgentSortField) {
    if (field === sortBy) {
      setSortDirection(prev => (prev === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortBy(field)
      setSortDirection('desc')
    }
    setPage(0)
  }

  function handleRemovalSuccess(target: 'revoke' | 'delete') {
    // Une révocation ne retire pas la ligne (l'agent reste listé en REVOKED) ; une
    // suppression la retire et peut vider la page courante.
    if (target === 'delete') {
      setPage(pageAfterRemoval(page, data?.content.length ?? 0, isPlaceholderData))
    }
    setRevokeTarget(null)
    setDeleteTarget(null)
  }

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

      <AgentStatsCards stats={stats} isLoading={statsPending} />

      {loadError && <Alert variant="error" className="mb-4">{t('load_error')}</Alert>}

      <SearchInput
        value={searchInput}
        onChange={handleSearchChange}
        placeholder={t('search.placeholder')}
        clearLabel={t('search.clear')}
        className="mb-3 max-w-md"
      />

      <div className="hidden md:block">
        <AgentsTable
          agents={agents}
          isLoading={isPending}
          sortBy={sortBy}
          sortDirection={sortDirection}
          onSort={handleSort}
          pendingActionId={pendingActionId}
          onRevoke={setRevokeTarget}
          onDelete={setDeleteTarget}
        />
      </div>
      <div className="md:hidden">
        <AgentsCardList
          agents={agents}
          isLoading={isPending}
          pendingActionId={pendingActionId}
          onRevoke={setRevokeTarget}
          onDelete={setDeleteTarget}
        />
      </div>

      {showPagination && (
        <div className="mt-3">
          <Pagination
            page={page}
            totalPages={totalPages}
            onPageChange={setPage}
            pageLabel={t('pagination.page', { current: page + 1, total: totalPages })}
            prevLabel={t('pagination.prev')}
            nextLabel={t('pagination.next')}
            summary={t('pagination.total', { count: totalElements })}
            isTransitioning={isPlaceholderData}
          />
        </div>
      )}

      {revokeTarget && (
        <RevokeAgentModal
          agent={revokeTarget}
          onClose={() => setRevokeTarget(null)}
          onRevoke={revokeAgent.mutateAsync}
          onSuccess={() => handleRemovalSuccess('revoke')}
        />
      )}

      {deleteTarget && (
        <DeleteAgentModal
          agent={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDelete={deleteAgent.mutateAsync}
          onSuccess={() => handleRemovalSuccess('delete')}
        />
      )}
    </div>
  )
}