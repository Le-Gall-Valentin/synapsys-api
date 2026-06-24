import { createContext, useContext, type ReactNode } from 'react'
import type { IAgentsApi } from './IAgentsApi'

const AgentsApiContext = createContext<IAgentsApi | null>(null)

interface AdminAgentsApiProviderProps {
  api: IAgentsApi
  children: ReactNode
}

/** Injecte l'implémentation IAgentsApi consommée par les hooks de la slice. */
export function AdminAgentsApiProvider({ api, children }: AdminAgentsApiProviderProps) {
  return <AgentsApiContext.Provider value={api}>{children}</AgentsApiContext.Provider>
}

export function useAgentsApi(): IAgentsApi {
  const api = useContext(AgentsApiContext)
  if (!api) {
    throw new Error('useAgentsApi must be used within an AdminAgentsApiProvider')
  }
  return api
}