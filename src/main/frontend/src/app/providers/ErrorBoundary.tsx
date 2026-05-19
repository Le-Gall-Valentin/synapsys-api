import { Component, type ErrorInfo, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

function ErrorFallback({ onReset }: { onReset: () => void }) {
  const { t } = useTranslation('common')
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-bg-0 px-4">
      <p className="text-sm text-fg-2">{t('error.unexpected')}</p>
      <button
        className="mt-4 text-xs text-accent underline"
        onClick={onReset}
        aria-label={t('error.retry')}
      >
        {t('error.retry')}
      </button>
    </div>
  )
}

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[ErrorBoundary]', error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      return <ErrorFallback onReset={() => this.setState({ hasError: false })} />
    }
    return this.props.children
  }
}