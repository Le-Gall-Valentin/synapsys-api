import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

export function ApplicationDetailPage() {
  const { t } = useTranslation('applicationDetail')
  const { appId } = useParams<{ appId: string }>()
  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold tracking-tight text-fg-0">
        {t('title')} {appId}
      </h1>
    </div>
  )
}