import type { AgentStatus } from '../model/IAgentsApi'

const STATUS_PILL_CLASS: Record<AgentStatus, string> = {
  ACTIVE: 'border-status-green/20 bg-status-green-dim text-status-green',
  INACTIVE: 'border-border-2 bg-bg-3 text-fg-2',
  PENDING: 'border-status-orange/20 bg-status-orange-dim text-status-orange',
  REVOKED: 'border-status-red/20 bg-status-red-dim text-status-red',
}

interface AgentStatusPillProps {
  status: AgentStatus
  /** Libellé traduit. */
  label: string
}

export function AgentStatusPill({ status, label }: AgentStatusPillProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-[7px] py-[2px] rounded-full border text-[10px] font-semibold font-mono uppercase tracking-[0.04em] whitespace-nowrap ${STATUS_PILL_CLASS[status]}`}
    >
      {status === 'ACTIVE' && <span className="size-1.5 rounded-full bg-status-green animate-pulse" aria-hidden="true" />}
      {label}
    </span>
  )
}