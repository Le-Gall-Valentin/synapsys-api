import { useTranslation } from 'react-i18next'

export function AccountPage() {
  const { t } = useTranslation('account')
  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold tracking-tight text-fg-0">{t('title')}</h1>
    </div>
  )
}