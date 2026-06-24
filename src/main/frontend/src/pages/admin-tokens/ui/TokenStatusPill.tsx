import type { EnrollmentTokenStatus } from '../model/IEnrollmentTokensApi'

const STATUS_PILL_CLASS: Record<EnrollmentTokenStatus, string> = {
  ACTIVE: 'border-status-green/20 bg-status-green-dim text-status-green',
  CONSUMED: 'border-border-2 bg-bg-3 text-fg-2',
  EXPIRED: 'border-status-red/20 bg-status-red-dim text-status-red',
  REVOKED: 'border-status-orange/20 bg-status-orange-dim text-status-orange',
}

interface TokenStatusPillProps {
  status: EnrollmentTokenStatus
  /** Translated label. */
  label: string
}

export function TokenStatusPill({ status, label }: TokenStatusPillProps) {
  return (
    <span
      className={`inline-flex items-center px-[7px] py-[2px] rounded-full border text-[10px] font-semibold font-mono uppercase tracking-[0.04em] whitespace-nowrap ${STATUS_PILL_CLASS[status]}`}
    >
      {label}
    </span>
  )
}
