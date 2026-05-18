import { Component, type ErrorInfo, type ReactNode } from 'react'
import i18n from '@/app/i18n'

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
      return (
        <div className="flex min-h-screen flex-col items-center justify-center bg-bg-0 px-4">
          <p className="text-sm text-fg-2">
            {i18n.t('error.unexpected', { ns: 'common' })}
          </p>
          <button
            className="mt-4 text-xs text-accent underline"
            onClick={() => this.setState({ hasError: false })}
            aria-label={i18n.t('error.retry', { ns: 'common' })}
          >
            {i18n.t('error.retry', { ns: 'common' })}
          </button>
        </div>
      )
    }
    return this.props.children
  }
}