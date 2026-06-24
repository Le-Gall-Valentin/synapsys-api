import { useTranslation } from 'react-i18next'
import { ChevronDown, ChevronUp, Server } from 'lucide-react'
import { formatRelativeTime } from '@/shared/lib'
import type { Agent, AgentSortField, SortDirection } from '../model/IAgentsApi'
import { formatAgentDate } from '../lib/formatAgentDate'
import { AgentStatusPill } from './AgentStatusPill'
import { AgentRowActions } from './AgentRowActions'

export interface AgentRowCallbacks {
  onRevoke: (agent: Agent) => void
  onDelete: (agent: Agent) => void
}

interface AgentsTableProps extends AgentRowCallbacks {
  agents: Agent[]
  isLoading: boolean
  sortBy: AgentSortField
  sortDirection: SortDirection
  onSort: (field: AgentSortField) => void
  /** Id de l'agent dont une mutation (revoke/delete) est en cours. */
  pendingActionId?: string | null
}

const HEAD_CLASS = 'px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wide text-fg-2'

function shortFingerprint(fp: string): string {
  // Affiche un extrait lisible de l'empreinte (le préfixe "SHA256:" est redondant ici).
  const body = fp.startsWith('SHA256:') ? fp.slice(7) : fp
  return body.length > 23 ? `${body.slice(0, 23)}…` : body
}

export function AgentsTable({ agents, isLoading, sortBy, sortDirection, onSort, pendingActionId, onRevoke, onDelete }: AgentsTableProps) {
  const { t, i18n } = useTranslation('adminAgents')

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

  const sortableHeader = (field: AgentSortField, labelKey: string) => {
    const active = sortBy === field
    // The label announces the order the NEXT click produces: an active asc column
    // flips to desc; an active desc column flips to asc; an inactive column starts
    // at desc (see toggleSort, which defaults a newly selected field to desc).
    const nextIsAsc = active && sortDirection === 'desc'
    const ariaLabel = nextIsAsc ? t('table.sort_asc') : t('table.sort_desc')
    return (
      <th className={HEAD_CLASS}>
        <button type="button" onClick={() => onSort(field)} aria-label={ariaLabel}
          className="inline-flex items-center gap-1 uppercase tracking-wide hover:text-fg-0">
          {t(labelKey)}
          {active && (sortDirection === 'asc' ? <ChevronUp className="size-3" aria-hidden="true" /> : <ChevronDown className="size-3" aria-hidden="true" />)}
        </button>
      </th>
    )
  }

  return (
    <div className="rounded-md border border-border bg-bg-1 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-bg-2">
              {sortableHeader('serverName', 'table.col_server')}
              <th className={HEAD_CLASS}>{t('table.col_address')}</th>
              <th className={HEAD_CLASS}>{t('table.col_status')}</th>
              <th className={HEAD_CLASS}>{t('table.col_fingerprint')}</th>
              {sortableHeader('enrolledAt', 'table.col_enrolled')}
              {sortableHeader('lastActivityAt', 'table.col_last_activity')}
              <th className="px-4 py-2.5" />
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {agents.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-sm text-fg-2">{t('table.empty')}</td></tr>
            )}
            {agents.map(agent => {
              return (
                <tr key={agent.id}>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center gap-2 font-mono font-medium text-fg-0">
                      <Server className="size-3.5 text-fg-3" aria-hidden="true" />
                      {agent.serverName}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-mono text-[11px] text-fg-2">{agent.ipAddress ?? t('table.none')}</td>
                  <td className="px-4 py-3"><AgentStatusPill status={agent.status} label={t(`status.${agent.status}`)} /></td>
                  <td className="px-4 py-3 font-mono text-[10px] text-fg-3">
                    {agent.fingerprint
                      ? <span title={agent.fingerprint}>{shortFingerprint(agent.fingerprint)}</span>
                      : <span className="text-fg-3 italic">{t('table.fingerprint_pending')}</span>}
                  </td>
                  <td className="px-4 py-3 font-mono text-[11px] text-fg-2">{formatAgentDate(agent.enrolledAt, i18n.language)}</td>
                  <td className="px-4 py-3 font-mono text-[11px] text-fg-2">
                    {agent.lastActivityAt ? formatRelativeTime(agent.lastActivityAt, i18n.language) : t('table.none')}
                  </td>
                  <td className="px-4 py-3">
                    <AgentRowActions agent={agent} pendingActionId={pendingActionId} onRevoke={onRevoke} onDelete={onDelete} />
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