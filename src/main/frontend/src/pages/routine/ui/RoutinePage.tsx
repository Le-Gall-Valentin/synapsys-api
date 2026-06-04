import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

export function RoutinePage() {
  const { t } = useTranslation('routine')
  const { routineId } = useParams<{ appId: string; routineId: string }>()
  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold tracking-tight text-fg-0">
        {t('title')} {routineId}
      </h1>
    </div>
  )
}