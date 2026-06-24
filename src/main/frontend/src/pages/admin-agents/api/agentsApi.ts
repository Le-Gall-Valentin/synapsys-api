import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { IAgentsApi, AgentsPage, AgentStatistics, AgentSortField, SortDirection } from '../model/IAgentsApi'

const BASE = '/agents'

/** Levée quand le backend refuse un revoke (déjà révoqué) ou un delete (agent non révoqué) : HTTP 409. */
export class AgentNotRevocableError extends Error {
  constructor() { super('Agent action conflicts with its current state'); this.name = 'AgentNotRevocableError' }
}

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    if (status === 404) throw new NotFoundError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const agentsApi: IAgentsApi = {
  async listAgents(page, size, sortBy: AgentSortField, sortDirection: SortDirection, search): Promise<AgentsPage> {
    try {
      const params: Record<string, string | number> = { page, size, sortBy, sortDirection }
      if (search) params.search = search
      const res = await client.get<AgentsPage>(BASE, { params })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async getStatistics(): Promise<AgentStatistics> {
    try {
      const res = await client.get<AgentStatistics>(`${BASE}/statistics`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async revokeAgent(id: string): Promise<void> {
    try {
      await client.post(`${BASE}/${id}/revoke`)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) throw new AgentNotRevocableError()
      handleError(error)
    }
  },

  async deleteAgent(id: string): Promise<void> {
    try {
      await client.delete(`${BASE}/${id}`)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) throw new AgentNotRevocableError()
      handleError(error)
    }
  },
}