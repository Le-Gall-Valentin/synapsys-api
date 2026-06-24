import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Copy } from 'lucide-react'
import { Alert, Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { CreatedToken } from '../model/IEnrollmentTokensApi'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'

const TTL_OPTIONS = [5, 15, 60, 1440] as const
const DEFAULT_TTL = 15
const SERVER_NAME_MAX = 100

// Verified server-side: the enrollment endpoint (POST /api/agents/enroll) and the
// agent WebSocket path (default /ws/agents). The deployment host is unknown from
// the browser, so it stays a placeholder for the operator to fill in.
const API_HOST_PLACEHOLDER = '<host>'
const ENROLL_ENDPOINT = `https://${API_HOST_PLACEHOLDER}/api/agents/enroll`
const WS_ENDPOINT = `wss://${API_HOST_PLACEHOLDER}/ws/agents`

interface GenerateTokenModalProps {
  onClose: () => void
  onCreate: (serverName: string, ttlMinutes: number) => Promise<CreatedToken>
  /** Called when the modal closes after a successful generation. */
  onSuccess: () => void
}

export function GenerateTokenModal({ onClose, onCreate, onSuccess }: GenerateTokenModalProps) {
  const { t } = useTranslation('adminTokens')
  const [serverName, setServerName] = useState('')
  const [ttl, setTtl] = useState<number>(DEFAULT_TTL)
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const [created, setCreated] = useState<CreatedToken | null>(null)
  const pendingRef = useRef(false)

  const trimmed = serverName.trim()
  const canSubmit = trimmed.length > 0 && trimmed.length <= SERVER_NAME_MAX

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      const result = await onCreate(trimmed, ttl)
      setCreated(result)
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'create'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    if (created) onSuccess()
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('create.title')} maxWidth="max-w-lg">
      <div className="mb-5">
        <h3 className="text-base font-semibold text-fg-0">{t('create.title')}</h3>
      </div>

      {!created ? (
        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <Input
              label={t('create.server_name')}
              name="serverName"
              value={serverName}
              onChange={e => setServerName(e.target.value)}
              placeholder={t('create.server_name_placeholder')}
              disabled={isLoading}
              maxLength={SERVER_NAME_MAX}
              autoFocus
            />
          </div>

          <div className="mb-1 flex flex-col gap-2">
            <label htmlFor="token-ttl" className="text-xs font-medium text-fg-1">{t('create.ttl')}</label>
            <select
              id="token-ttl"
              value={ttl}
              onChange={e => setTtl(Number(e.target.value))}
              disabled={isLoading}
              className="w-full rounded-lg border border-border bg-bg-1 px-3.5 py-3 text-sm text-fg-0 outline-none transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_var(--color-accent-ring)] disabled:opacity-50"
            >
              {TTL_OPTIONS.map(option => (
                <option key={option} value={option}>{t(`create.ttl_option.${option}`)}</option>
              ))}
            </select>
          </div>

          {errorKey && <Alert variant="error" className="mt-3">{t(errorKey)}</Alert>}

          <div className="flex justify-end gap-2 mt-5">
            <Button type="button" onClick={handleClose} disabled={isLoading}>{t('create.cancel')}</Button>
            <Button type="submit" disabled={!canSubmit} isLoading={isLoading} className="border-transparent font-semibold" style={CTA_BUTTON_STYLE}>
              {t('create.submit')}
            </Button>
          </div>
        </form>
      ) : (
        <div>
          <Alert variant="warning" className="mb-3">{t('create.copy_now')}</Alert>
          <p className="text-xs text-fg-2 mb-1">{t('create.token_label')}</p>
          <div className="mb-3 rounded-lg border border-border bg-bg-2 p-3 font-mono text-xs text-accent break-all">
            {created.token}
          </div>
          <p className="text-xs text-fg-2 mb-1">{t('create.endpoints_label')}</p>
          <div className="mb-4 space-y-2 rounded-lg border border-border bg-bg-2 p-3 font-mono text-[11px] text-fg-1">
            <div>
              <div className="text-fg-3">{t('create.enroll_endpoint')}</div>
              <div className="break-all">POST {ENROLL_ENDPOINT}</div>
            </div>
            <div>
              <div className="text-fg-3">{t('create.ws_endpoint')}</div>
              <div className="break-all">{WS_ENDPOINT}</div>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" onClick={() => { void navigator.clipboard?.writeText(created.token) }}>
              <Copy className="size-4" />
              {t('create.copy')}
            </Button>
            <Button type="button" onClick={handleClose} className="border-transparent font-semibold" style={CTA_BUTTON_STYLE}>
              {t('create.close')}
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  )
}
