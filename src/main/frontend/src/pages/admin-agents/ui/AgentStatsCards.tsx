import { useTranslation } from 'react-i18next'
import type { AgentStatistics } from '../model/IAgentsApi'

interface AgentStatsCardsProps {
  stats: AgentStatistics | undefined
  isLoading: boolean
}

interface CardSpec {
  key: keyof Pick<AgentStatistics, 'active' | 'inactive' | 'pending' | 'revoked'>
  labelKey: string
  hintKey: string
  valueClass: string
}

const CARDS: CardSpec[] = [
  { key: 'active', labelKey: 'stats.active', hintKey: 'stats.active_hint', valueClass: 'text-status-green' },
  { key: 'inactive', labelKey: 'stats.inactive', hintKey: 'stats.inactive_hint', valueClass: 'text-fg-1' },
  { key: 'pending', labelKey: 'stats.pending', hintKey: 'stats.pending_hint', valueClass: 'text-status-orange' },
  { key: 'revoked', labelKey: 'stats.revoked', hintKey: 'stats.revoked_hint', valueClass: 'text-status-red' },
]

export function AgentStatsCards({ stats, isLoading }: AgentStatsCardsProps) {
  const { t } = useTranslation('adminAgents')
  return (
    <div className="grid grid-cols-2 gap-3 mb-4 lg:grid-cols-4">
      {CARDS.map(card => (
        <div key={card.key} className="rounded-lg border border-border bg-bg-1 p-4">
          <div className="text-[11px] font-semibold uppercase tracking-wide text-fg-2">{t(card.labelKey)}</div>
          <div className={`mt-1 text-2xl font-semibold tabular-nums ${card.valueClass}`}>
            {isLoading || !stats ? <span className="text-fg-3">—</span> : stats[card.key]}
          </div>
          <div className="mt-1 text-[11px] text-fg-3">{t(card.hintKey)}</div>
        </div>
      ))}
    </div>
  )
}