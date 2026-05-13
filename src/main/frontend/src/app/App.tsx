import { QueryProvider } from './providers'
import { AppRouter } from './router'

export function App() {
  return (
    <QueryProvider>
      <AppRouter />
    </QueryProvider>
  )
}