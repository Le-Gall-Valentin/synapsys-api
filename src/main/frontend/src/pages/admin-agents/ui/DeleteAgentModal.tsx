import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Trash2 } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
import type { Agent } from '../model/IAgentsApi'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'

interface DeleteAgentModalProps {
  agent: Agent
  onClose: () => void
  onDelete: (id: string) => Promise<void>
  onSuccess: () => void
}

export function DeleteAgentModal({ agent, onClose, onDelete, onSuccess }: DeleteAgentModalProps) {
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
      await onDelete(agent.id)
      onSuccess()
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'delete'))
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
    <Dialog open onClose={handleClose} title={t('delete.title', { server: agent.serverName })}>
      <div className="mb-5">
        <h3 className="text-base font-semibold text-fg-0 mb-1">{t('delete.title', { server: agent.serverName })}</h3>
        <p className="text-sm text-fg-2">{t('delete.body')}</p>
      </div>

      <div className="mb-5 rounded-lg border border-status-red/20 bg-status-red-dim px-3.5 py-2.5 text-sm text-status-red">
        <span className="font-medium font-mono">{agent.serverName}</span>
      </div>

      {errorKey && <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={handleClose} disabled={isLoading}>{t('delete.cancel')}</Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-status-red/30 bg-status-red-dim text-status-red hover:bg-status-red/20"
        >
          <Trash2 className="size-4" />
          {t('delete.submit')}
        </Button>
      </div>
    </Dialog>
  )
}