import { ThemeProvider, LanguageProvider, AuthProvider, ErrorBoundary } from './providers'
import { AppRouter } from './router'

export function App() {
  return (
    <ThemeProvider>
      <LanguageProvider>
        <AuthProvider>
          <ErrorBoundary>
            <AppRouter />
          </ErrorBoundary>
        </AuthProvider>
      </LanguageProvider>
    </ThemeProvider>
  )
}