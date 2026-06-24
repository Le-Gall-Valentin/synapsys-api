import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import type { Agent } from '../model/IAgentsApi'

interface AgentRowActionsProps {
  agent: Agent
  /** Id of the agent whose revoke/delete mutation is in flight (disables its button). */
  pendingActionId?: string | null
  onRevoke: (agent: Agent) => void
  onDelete: (agent: Agent) => void
  /** Extra classes on the action container (e.g. top margin in the card layout). */
  className?: string
}

/**
 * Status-gated destructive actions for one agent row, shared by the table and the
 * card list so the gating rules live in a single place: revoke for ACTIVE/INACTIVE,
 * delete for REVOKED, nothing for PENDING.
 */
export function AgentRowActions({ agent, pendingActionId, onRevoke, onDelete, className = '' }: AgentRowActionsProps) {
  const { t } = useTranslation('adminAgents')
  const canRevoke = agent.status === 'ACTIVE' || agent.status === 'INACTIVE'
  const canDelete = agent.status === 'REVOKED'
  if (!canRevoke && !canDelete) return null

  const isPending = agent.id === pendingActionId
  return (
    <div className={`flex justify-end gap-2 ${className}`}>
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
  )
}