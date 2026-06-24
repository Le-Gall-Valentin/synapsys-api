import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AgentStatsCards } from './AgentStatsCards'
import type { AgentStatistics } from '../model/IAgentsApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k, i18n: { language: 'fr' } }),
}))

const STATS: AgentStatistics = { active: 3, inactive: 1, pending: 2, revoked: 4, total: 10 }

describe('AgentStatsCards', () => {
  it('renders the four counters when stats are present', () => {
    render(<AgentStatsCards stats={STATS} isLoading={false} />)
    expect(screen.getByText('3')).toBeDefined()
    expect(screen.getByText('1')).toBeDefined()
    expect(screen.getByText('2')).toBeDefined()
    expect(screen.getByText('4')).toBeDefined()
    expect(screen.queryByText('stats.load_error')).toBeNull()
  })

  it('shows dashes and no error note while loading', () => {
    render(<AgentStatsCards stats={undefined} isLoading />)
    expect(screen.getAllByText('—')).toHaveLength(4)
    expect(screen.queryByText('stats.load_error')).toBeNull()
  })

  it('surfaces an error note when the statistics query failed', () => {
    render(<AgentStatsCards stats={undefined} isLoading={false} isError />)
    expect(screen.getByText('stats.load_error')).toBeDefined()
    expect(screen.getAllByText('—')).toHaveLength(4)
  })
})