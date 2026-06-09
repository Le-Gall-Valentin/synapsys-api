import { AlertTriangle, CheckCircle2 } from 'lucide-react'

const VARIANT_CLASS = {
  error: 'border-status-red/25 bg-status-red-dim text-status-red',
  warning: 'border-status-orange/25 bg-status-orange-dim text-status-orange',
  success: 'border-status-green/25 bg-status-green-dim text-status-green',
} as const

type AlertVariant = keyof typeof VARIANT_CLASS

interface AlertProps {
  variant: AlertVariant
  children: React.ReactNode
  /** Renders a dismiss button when provided. */
  onDismiss?: () => void
  dismissLabel?: string
  className?: string
}

export function Alert({ variant, children, onDismiss, dismissLabel, className = '' }: AlertProps) {
  const Icon = variant === 'success' ? CheckCircle2 : AlertTriangle
  return (
    <div
      role={variant === 'error' ? 'alert' : 'status'}
      className={`flex items-start gap-2.5 rounded-lg border px-3.5 py-2.5 text-sm ${VARIANT_CLASS[variant]} ${className}`}
    >
      <Icon className="size-4 shrink-0 mt-0.5" aria-hidden="true" />
      <span className="flex-1">{children}</span>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label={dismissLabel}
          className="shrink-0 opacity-70 hover:opacity-100"
        >
          ×
        </button>
      )}
    </div>
  )
}
