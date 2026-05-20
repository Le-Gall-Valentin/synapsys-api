import { Loader2 } from 'lucide-react'

export function Spinner({ label = 'Loading' }: { label?: string } = {}) {
  return (
    <div
      className="flex min-h-screen items-center justify-center bg-bg-0"
      role="status"
      aria-label={label}
    >
      <Loader2 className="size-8 animate-spin text-accent" aria-hidden="true" />
    </div>
  )
}