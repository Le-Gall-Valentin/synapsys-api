import { AuthProvider, QueryProvider } from './providers'
import { AppRouter } from './router'

export function App() {
  return (
    <QueryProvider>
      <AuthProvider>
        <AppRouter />
      </AuthProvider>
    </QueryProvider>
  )
}
