import type { ReactNode } from 'react'
import { authApi, AuthStoreProvider } from '@/features/auth'

export function AuthProvider({ children }: { children: ReactNode }) {
  return <AuthStoreProvider api={authApi}>{children}</AuthStoreProvider>
}