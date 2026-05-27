import { AuthProvider, ErrorBoundary } from './providers'
import { AppRouter } from './router'

export function App() {
  return (
    <AuthProvider>
      <ErrorBoundary>
        <AppRouter />
      </ErrorBoundary>
    </AuthProvider>
  )
}