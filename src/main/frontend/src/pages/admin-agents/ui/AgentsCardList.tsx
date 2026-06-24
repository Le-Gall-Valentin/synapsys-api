import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import { formatRelativeTime } from '@/shared/lib'
import type { Agent } from '../model/IAgentsApi'
import type { AgentRowCallbacks } from './AgentsTable'
import { formatAgentDate } from '../lib/formatAgentDate'
import { AgentStatusPill } from './AgentStatusPill'

interface AgentsCardListProps extends AgentRowCallbacks {
  agents: Agent[]
  isLoading: boolean
  pendingActionId?: string | null
}

export function AgentsCardList({ agents, isLoading, pendingActionId, onRevoke, onDelete }: AgentsCardListProps) {
  const { t, i18n } = useTranslation('adminAgents')

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-24 rounded-lg border border-border bg-bg-1 animate-pulse" />
        ))}
      </div>
    )
  }

  if (agents.length === 0) {
    return <div className="rounded-lg border border-border bg-bg-1 px-4 py-8 text-center text-sm text-fg-2">{t('table.empty')}</div>
  }

  return (
    <div className="space-y-2">
      {agents.map(agent => {
        const canRevoke = agent.status === 'ACTIVE' || agent.status === 'INACTIVE'
        const canDelete = agent.status === 'REVOKED'
        const isPending = agent.id === pendingActionId
        return (
          <div key={agent.id} className="rounded-lg border border-border bg-bg-1 p-4">
            <div className="flex items-start justify-between gap-2">
              <span className="font-mono font-medium text-fg-0">{agent.serverName}</span>
              <AgentStatusPill status={agent.status} label={t(`status.${agent.status}`)} />
            </div>
            <dl className="mt-3 grid grid-cols-2 gap-y-1.5 text-[11px]">
              <dt className="text-fg-3">{t('table.col_address')}</dt>
              <dd className="font-mono text-right text-fg-2">{agent.ipAddress ?? t('table.none')}</dd>
              <dt className="text-fg-3">{t('table.col_enrolled')}</dt>
              <dd className="font-mono text-right text-fg-2">{formatAgentDate(agent.enrolledAt, i18n.language)}</dd>
              <dt className="text-fg-3">{t('table.col_last_activity')}</dt>
              <dd className="font-mono text-right text-fg-2">
                {agent.lastActivityAt ? formatRelativeTime(agent.lastActivityAt, i18n.language) : t('table.none')}
              </dd>
            </dl>
            {(canRevoke || canDelete) && (
              <div className="mt-3 flex justify-end gap-2">
                {canRevoke && (
                  <button type="button" onClick={() => onRevoke(agent)} disabled={isPending}
                    className="flex items-center gap-1.5 rounded-md border border-status-red/30 bg-status-red-dim px-2.5 py-1.5 text-xs font-medium text-status-red transition-colors hover:bg-status-red/20 disabled:opacity-50">
                    {isPending && <Loader2 className="size-3.5 animate-spin" aria-hidden="true" />}
                    {t('table.revoke')}
                  </button>
                )}
                {canDelete && (
                  <button type="button" onClick={() => onDelete(agent)} disabled={isPending}
                    className="flex items-center gap-1.5 rounded-md border border-border-2 bg-bg-2 px-2.5 py-1.5 text-xs font-medium text-fg-1 transition-colors hover:bg-bg-3 hover:text-fg-0 disabled:opacity-50">
                    {isPending && <Loader2 className="size-3.5 animate-spin" aria-hidden="true" />}
                    {t('table.delete')}
                  </button>
                )}
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}