import { useState } from 'react'
import { useDebouncedValue, pageAfterRemoval } from '@/shared/lib'
import type { Agent, AgentSortField, SortDirection } from './IAgentsApi'
import { useAgents, AGENTS_PAGE_SIZE } from './useAgents'
import { useAgentStatistics } from './useAgentStatistics'
import { useRevokeAgent, useDeleteAgent } from './useAgentMutations'

/**
 * View-model for the agents admin page: owns pagination/sort/search state (with its
 * debounce), the list and statistics queries, the revoke/delete mutations and the
 * confirmation targets. Keeping this here leaves the page purely presentational (SRP).
 */
export function useAgentsPage() {
  const [page, setPage] = useState(0)
  const [sortBy, setSortBy] = useState<AgentSortField>('enrolledAt')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [searchInput, setSearchInput] = useState('')
  const search = useDebouncedValue(searchInput, 300)

  const [revokeTarget, setRevokeTarget] = useState<Agent | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Agent | null>(null)

  const list = useAgents(page, sortBy, sortDirection, search)
  const statistics = useAgentStatistics()
  const revokeAgent = useRevokeAgent()
  const deleteAgent = useDeleteAgent()

  const agents = list.data?.content ?? []
  const totalElements = list.data?.totalElements ?? 0
  const pageSize = list.data?.size ?? AGENTS_PAGE_SIZE
  const totalPages = totalElements > 0 ? Math.ceil(totalElements / pageSize) : 1
  const pendingActionId = revokeAgent.isPending ? revokeAgent.variables
    : deleteAgent.isPending ? deleteAgent.variables : null

  function changeSearch(value: string) {
    setSearchInput(value)
    setPage(0)
  }

  function toggleSort(field: AgentSortField) {
    if (field === sortBy) {
      setSortDirection(prev => (prev === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortBy(field)
      setSortDirection('desc')
    }
    setPage(0)
  }

  // A revoke keeps the agent listed (now REVOKED); a delete removes the row and can
  // empty the current page, so only delete adjusts the page.
  function onRevokeSuccess() {
    setRevokeTarget(null)
  }
  function onDeleteSuccess() {
    setPage(pageAfterRemoval(page, list.data?.content.length ?? 0, list.isPlaceholderData))
    setDeleteTarget(null)
  }

  return {
    // list view-model
    agents,
    totalElements,
    totalPages,
    isPending: list.isPending,
    loadError: list.isError,
    isPlaceholderData: list.isPlaceholderData,
    // statistics view-model
    stats: statistics.data,
    statsPending: statistics.isPending,
    statsError: statistics.isError,
    // pagination + sort + search state
    page,
    setPage,
    sortBy,
    sortDirection,
    searchInput,
    changeSearch,
    toggleSort,
    // row action state
    pendingActionId,
    revokeTarget,
    setRevokeTarget,
    deleteTarget,
    setDeleteTarget,
    revokeAgent,
    deleteAgent,
    onRevokeSuccess,
    onDeleteSuccess,
  }
}