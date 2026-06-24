import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import type { EnrollmentToken } from '../model/IEnrollmentTokensApi'
import { formatRelativeTime } from '../lib/formatRelativeTime'
import { formatExpiry } from '../lib/formatExpiry'
import { TokenStatusPill } from './TokenStatusPill'

export interface TokenRowCallbacks {
  onRevoke: (token: EnrollmentToken) => void
}

interface TokensTableProps extends TokenRowCallbacks {
  tokens: EnrollmentToken[]
  isLoading: boolean
  /** Id of the token whose revoke is currently in flight. */
  pendingRevokeId?: string | null
}

export function TokensTable({ tokens, isLoading, pendingRevokeId, onRevoke }: TokensTableProps) {
  const { t, i18n } = useTranslation('adminTokens')

  if (isLoading) {
    return (
      <div className="rounded-md border border-border bg-bg-1 overflow-hidden">
        <div className="divide-y divide-border">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-4 py-3">
              <div className="h-3.5 w-32 bg-bg-3 animate-pulse rounded" />
              <div className="h-3 w-40 bg-bg-3 animate-pulse rounded ml-auto" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-md border border-border bg-bg-1 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-bg-2">
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_server')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_status')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_expires')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_created_by')}</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t('table.col_created')}</th>
              <th className="px-4 py-2.5" />
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {tokens.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-sm text-fg-2">{t('table.empty')}</td>
              </tr>
            )}
            {tokens.map(token => {
              return (
                <tr key={token.id}>
                  <td className="px-4 py-3 font-medium text-fg-0">{token.serverName}</td>
                  <td className="px-4 py-3">
                    <TokenStatusPill status={token.status} label={t(`status.${token.status}`)} />
                  </td>
                  <td className="px-4 py-3 font-mono text-[11px] text-fg-2">
                    {token.status === 'EXPIRED' ? t('table.expired') : formatExpiry(token.expiresAt, i18n.language)}
                  </td>
                  <td className="px-4 py-3 text-fg-2">{token.createdBy.username}</td>
                  <td className="px-4 py-3 font-mono text-[11px] text-fg-2">
                    {formatRelativeTime(token.createdAt, i18n.language)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end">
                      {token.status === 'ACTIVE' && (
                        <button
                          type="button"
                          onClick={() => onRevoke(token)}
                          disabled={token.id === pendingRevokeId}
                          className="flex items-center gap-1.5 rounded-md border border-status-red/30 bg-status-red-dim px-2.5 py-1.5 text-xs font-medium text-status-red transition-colors hover:bg-status-red/20 disabled:opacity-50"
                        >
                          {token.id === pendingRevokeId && <Loader2 className="size-3.5 animate-spin" aria-hidden="true" />}
                          {t('table.revoke')}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
