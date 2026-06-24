import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import type { EnrollmentToken } from '../model/IEnrollmentTokensApi'
import { formatRelativeTime } from '../lib/formatRelativeTime'
import { formatExpiry } from '../lib/formatExpiry'
import { TokenStatusPill } from './TokenStatusPill'
import type { TokenRowCallbacks } from './TokensTable'

interface TokensCardListProps extends TokenRowCallbacks {
  tokens: EnrollmentToken[]
  isLoading: boolean
  pendingRevokeId?: string | null
}

export function TokensCardList({ tokens, isLoading, pendingRevokeId, onRevoke }: TokensCardListProps) {
  const { t, i18n } = useTranslation('adminTokens')

  if (isLoading) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-24 rounded-md border border-border bg-bg-1 animate-pulse" />
        ))}
      </div>
    )
  }

  if (tokens.length === 0) {
    return <div className="rounded-md border border-border bg-bg-1 px-4 py-8 text-center text-sm text-fg-2">{t('table.empty')}</div>
  }

  return (
    <div className="flex flex-col gap-2">
      {tokens.map(token => {
        const expiry = formatExpiry(token.expiresAt, token.status, i18n.language)
        return (
          <div key={token.id} className="rounded-md border border-border bg-bg-1 p-4">
            <div className="flex items-start justify-between gap-2">
              <span className="font-medium text-fg-0">{token.serverName}</span>
              <TokenStatusPill status={token.status} label={t(`status.${token.status}`)} />
            </div>
            <dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-xs text-fg-2">
              <dt className="text-fg-3">{t('table.col_expires')}</dt>
              <dd className="font-mono text-right">{expiry === '__expired__' ? t('table.expired') : expiry}</dd>
              <dt className="text-fg-3">{t('table.col_created_by')}</dt>
              <dd className="text-right">{token.createdBy.username}</dd>
              <dt className="text-fg-3">{t('table.col_created')}</dt>
              <dd className="font-mono text-right">{formatRelativeTime(token.createdAt, i18n.language)}</dd>
            </dl>
            {token.status === 'ACTIVE' && (
              <button
                type="button"
                onClick={() => onRevoke(token)}
                disabled={token.id === pendingRevokeId}
                className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-md border border-status-red/30 bg-status-red-dim px-2.5 py-1.5 text-xs font-medium text-status-red transition-colors hover:bg-status-red/20 disabled:opacity-50"
              >
                {token.id === pendingRevokeId && <Loader2 className="size-3.5 animate-spin" aria-hidden="true" />}
                {t('table.revoke')}
              </button>
            )}
          </div>
        )
      })}
    </div>
  )
}
