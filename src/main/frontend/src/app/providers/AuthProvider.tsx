import { useEffect, useRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { setSessionExpiredCallback } from '@/shared/lib'
import { Spinner } from '@/shared/ui'

export function AuthProvider({ children }: { children: ReactNode }) {
  const isInitializing = useAuth((s) => s.isInitializing)
  const { t } = useTranslation('common')
  const started = useRef(false)

  useEffect(() => {
    if (started.current) return
    started.current = true
    setSessionExpiredCallback(() => void useAuth.getState().logout())
    void useAuth.getState().initialize()
  }, [])

  if (isInitializing) return <Spinner label={t('loading')} />
  return <>{children}</>
}