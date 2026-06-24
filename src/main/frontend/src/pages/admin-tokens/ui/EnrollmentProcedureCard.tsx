import { useTranslation } from 'react-i18next'
import { ShieldCheck, AlertTriangle } from 'lucide-react'

export function EnrollmentProcedureCard() {
  const { t } = useTranslation('adminTokens')
  return (
    <div className="mb-4 rounded-lg border border-border bg-bg-2 p-4">
      <div className="flex gap-3">
        <ShieldCheck className="size-5 shrink-0 text-accent" aria-hidden="true" />
        <div className="text-xs leading-relaxed">
          <p className="font-medium text-fg-1 mb-1">{t('procedure.title')}</p>
          <p className="text-fg-2">{t('procedure.body')}</p>
          <div className="mt-3 flex gap-2 border-t border-border pt-3 text-status-orange">
            <AlertTriangle className="size-4 shrink-0 mt-px" aria-hidden="true" />
            <p>{t('procedure.disclaimer')}</p>
          </div>
        </div>
      </div>
    </div>
  )
}
