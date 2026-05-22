import type { ReactNode } from 'react'
import { authApi } from '@/features/auth/api/authApi'
import { AuthStoreProvider } from '@/features/auth'

export function AuthProvider({ children }: { children: ReactNode }) {
  return <AuthStoreProvider api={authApi}>{children}</AuthStoreProvider>
}