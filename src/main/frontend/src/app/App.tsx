import { AuthProvider, ErrorBoundary, QueryProvider } from './providers'
import { AppRouter } from './router'

export function App() {
  return (
    <ErrorBoundary>
      <QueryProvider>
        <AuthProvider>
          <AppRouter />
        </AuthProvider>
      </QueryProvider>
    </ErrorBoundary>
  )
}