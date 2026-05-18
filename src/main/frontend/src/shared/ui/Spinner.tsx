import { Loader2 } from 'lucide-react'

interface SpinnerProps {
  ariaLabel?: string
}

export function Spinner({ ariaLabel = 'Loading...' }: SpinnerProps) {
  return (
    <div
      className="flex min-h-screen items-center justify-center bg-bg-0"
      role="status"
      aria-label={ariaLabel}
    >
      <Loader2 className="size-8 animate-spin text-accent" aria-hidden="true" />
    </div>
  )
}