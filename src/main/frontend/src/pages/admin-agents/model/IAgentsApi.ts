export type AgentStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'REVOKED'
export type AgentSortField = 'enrolledAt' | 'serverName' | 'lastActivityAt'
export type SortDirection = 'asc' | 'desc'

/** Agent enrôlé tel que listé par le backend (miroir de `AgentResponse`). */
export interface Agent {
  id: string
  serverName: string
  ipAddress: string | null
  status: AgentStatus
  fingerprint: string | null
  enrolledAt: string
  lastActivityAt: string | null
}

/** Compteurs globaux (miroir de `AgentStatisticsResponse`), jamais filtrés par la recherche. */
export interface AgentStatistics {
  active: number
  inactive: number
  pending: number
  revoked: number
  total: number
}

/** Payload paginé (miroir de `PageResponse`). */
export interface AgentsPage {
  content: Agent[]
  totalElements: number
  page: number
  size: number
}

/**
 * Port de gestion des agents. Les hooks dépendent de ce contrat, jamais de
 * l'implémentation axios concrète, injectée via AdminAgentsApiProvider.
 */
export interface IAgentsApi {
  listAgents(
    page: number,
    size: number,
    sortBy: AgentSortField,
    sortDirection: SortDirection,
    search?: string,
  ): Promise<AgentsPage>
  getStatistics(): Promise<AgentStatistics>
  revokeAgent(id: string): Promise<void>
  deleteAgent(id: string): Promise<void>
}