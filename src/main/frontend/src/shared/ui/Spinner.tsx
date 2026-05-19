import { Loader2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'

export function Spinner() {
  const { t } = useTranslation('common')
  return (
    <div
      className="flex min-h-screen items-center justify-center bg-bg-0"
      role="status"
      aria-label={t('loading')}
    >
      <Loader2 className="size-8 animate-spin text-accent" aria-hidden="true" />
    </div>
  )
}