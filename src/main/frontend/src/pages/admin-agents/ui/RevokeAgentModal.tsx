import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Ban } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
import type { Agent } from '../model/IAgentsApi'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'

interface RevokeAgentModalProps {
  agent: Agent
  onClose: () => void
  onRevoke: (id: string) => Promise<void>
  onSuccess: () => void
}

export function RevokeAgentModal({ agent, onClose, onRevoke, onSuccess }: RevokeAgentModalProps) {
  const { t } = useTranslation('adminAgents')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  async function handleSubmit() {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onRevoke(agent.id)
      onSuccess()
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'revoke'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    setErrorKey(null)
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('revoke.title', { server: agent.serverName })}>
      <div className="mb-5">
        <h3 className="text-base font-semibold text-fg-0 mb-1">{t('revoke.title', { server: agent.serverName })}</h3>
        <p className="text-sm text-fg-2">{t('revoke.body')}</p>
      </div>

      <div className="mb-5 rounded-lg border border-status-red/20 bg-status-red-dim px-3.5 py-2.5 text-sm text-status-red">
        <span className="font-medium font-mono">{agent.serverName}</span>
      </div>

      {errorKey && <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={handleClose} disabled={isLoading}>{t('revoke.cancel')}</Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-status-red/30 bg-status-red-dim text-status-red hover:bg-status-red/20"
        >
          <Ban className="size-4" />
          {t('revoke.submit')}
        </Button>
      </div>
    </Dialog>
  )
}