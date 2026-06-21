import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AdminUsersApiProvider, adminUsersApi } from '@/pages/admin-users'
import { ThemeProvider, LanguageProvider, AuthProvider, ErrorBoundary } from './providers'
import { AppRouter } from './router'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
    mutations: { retry: 0 },
  },
})

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AdminUsersApiProvider api={adminUsersApi}>
        <ThemeProvider>
          <LanguageProvider>
            <AuthProvider>
              <ErrorBoundary>
                <AppRouter />
              </ErrorBoundary>
            </AuthProvider>
          </LanguageProvider>
        </ThemeProvider>
      </AdminUsersApiProvider>
    </QueryClientProvider>
  )
}
