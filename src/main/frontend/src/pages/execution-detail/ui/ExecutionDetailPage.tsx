import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

export function ExecutionDetailPage() {
  const { t } = useTranslation('executionDetail')
  const { execId } = useParams<{ execId: string }>()
  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold tracking-tight text-fg-0">
        {t('title')} {execId}
      </h1>
    </div>
  )
}