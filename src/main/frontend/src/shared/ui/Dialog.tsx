import { useEffect, useRef, type ReactNode } from 'react'
import { useFocusTrap } from '@/shared/lib'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  maxWidth?: string
}

export function Dialog({ open, onClose, title, children, maxWidth = 'max-w-md' }: DialogProps) {
  const panelRef = useRef<HTMLDivElement>(null)
  useFocusTrap(panelRef, open)

  useEffect(() => {
    if (!open) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, onClose])

  if (!open) return null

  return (
    <div role="dialog" aria-modal="true" aria-labelledby="dialog-title" className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        aria-label="backdrop"
        className="fixed inset-0 bg-black/50"
        onClick={onClose}
      />
      <div ref={panelRef} className={`relative z-10 w-full ${maxWidth} rounded-xl border border-border bg-bg-1 p-6 shadow-xl`}>
        <h2 id="dialog-title" className="sr-only">{title}</h2>
        {children}
      </div>
    </div>
  )
}