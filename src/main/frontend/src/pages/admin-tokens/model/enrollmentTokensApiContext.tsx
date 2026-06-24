import { createContext, useContext, type ReactNode } from 'react'
import type { IEnrollmentTokensApi } from './IEnrollmentTokensApi'

const EnrollmentTokensApiContext = createContext<IEnrollmentTokensApi | null>(null)

interface AdminTokensApiProviderProps {
  api: IEnrollmentTokensApi
  children: ReactNode
}

/** Injects the IEnrollmentTokensApi implementation consumed by the slice's hooks. */
export function AdminTokensApiProvider({ api, children }: AdminTokensApiProviderProps) {
  return <EnrollmentTokensApiContext.Provider value={api}>{children}</EnrollmentTokensApiContext.Provider>
}

export function useEnrollmentTokensApi(): IEnrollmentTokensApi {
  const api = useContext(EnrollmentTokensApiContext)
  if (!api) {
    throw new Error('useEnrollmentTokensApi must be used within an AdminTokensApiProvider')
  }
  return api
}
