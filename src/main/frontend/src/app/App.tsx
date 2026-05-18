import { AuthProvider, ErrorBoundary } from './providers'
import { AppRouter } from './router'

export function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <AppRouter />
      </AuthProvider>
    </ErrorBoundary>
  )
}