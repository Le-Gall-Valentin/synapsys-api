import { render, within } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { TokensTable } from './TokensTable'
import type { EnrollmentToken } from '../model/IEnrollmentTokensApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k, i18n: { language: 'fr' } }),
}))

const ACTIVE_TOKEN: EnrollmentToken = {
  id: 't-1',
  serverName: 'web-01',
  status: 'ACTIVE',
  expiresAt: '2999-01-01T00:00:00Z',
  createdBy: { id: 'u-1', username: 'alice' },
  createdAt: '2026-06-24T11:00:00Z',
}

const CONSUMED_TOKEN: EnrollmentToken = {
  id: 't-2',
  serverName: 'db-01',
  status: 'CONSUMED',
  expiresAt: '2026-06-20T00:00:00Z',
  createdBy: { id: 'u-1', username: 'alice' },
  createdAt: '2026-06-20T11:00:00Z',
}

const EXPIRED_TOKEN: EnrollmentToken = {
  id: 't-3',
  serverName: 'cache-01',
  status: 'EXPIRED',
  expiresAt: '2026-01-01T00:00:00Z',
  createdBy: { id: 'u-2', username: 'bob' },
  createdAt: '2026-01-01T11:00:00Z',
}

const REVOKED_TOKEN: EnrollmentToken = {
  id: 't-4',
  serverName: 'queue-01',
  status: 'REVOKED',
  expiresAt: '2999-01-01T00:00:00Z',
  createdBy: { id: 'u-2', username: 'bob' },
  createdAt: '2026-06-22T11:00:00Z',
}

const DEFAULT_HANDLERS = {
  onRevoke: vi.fn(),
}

function setup(tokens: EnrollmentToken[] = [], isLoading = false, pendingRevokeId?: string | null) {
  return render(
    <TokensTable
      tokens={tokens}
      isLoading={isLoading}
      pendingRevokeId={pendingRevokeId}
      {...DEFAULT_HANDLERS}
    />
  )
}

beforeEach(() => { vi.clearAllMocks() })

describe('TokensTable — loading', () => {
  it('renders skeleton rows when isLoading', () => {
    const { container } = setup([], true)
    const skeletonRows = container.querySelectorAll('.animate-pulse')
    expect(skeletonRows.length).toBeGreaterThan(0)
  })

  it('does not render table when isLoading', () => {
    const { queryByRole } = setup([], true)
    expect(queryByRole('table')).toBeNull()
  })
})

describe('TokensTable — empty state', () => {
  it('renders table.empty when no tokens', () => {
    const { getByText } = setup([])
    expect(getByText('table.empty')).toBeDefined()
  })

  it('does not render table.empty when tokens exist', () => {
    const { queryByText } = setup([ACTIVE_TOKEN])
    expect(queryByText('table.empty')).toBeNull()
  })
})

describe('TokensTable — rows', () => {
  it('renders a row for each token', () => {
    const tokens = [ACTIVE_TOKEN, CONSUMED_TOKEN, EXPIRED_TOKEN]
    const { getAllByRole } = setup(tokens)
    const rows = getAllByRole('row')
    expect(rows.length).toBe(tokens.length + 1) // +1 for header
  })

  it('renders the server name in each row', () => {
    const tokens = [ACTIVE_TOKEN, CONSUMED_TOKEN]
    const { getAllByRole } = setup(tokens)
    const rows = getAllByRole('row').slice(1) // skip header
    expect(within(rows[0]).getByText('web-01')).toBeDefined()
    expect(within(rows[1]).getByText('db-01')).toBeDefined()
  })
})

describe('TokensTable — revoke button (CRITICAL: gated on ACTIVE status)', () => {
  it('renders exactly one revoke button for a single ACTIVE token', () => {
    const { getAllByText } = setup([ACTIVE_TOKEN])
    const revokeBtns = getAllByText('table.revoke')
    expect(revokeBtns.length).toBe(1)
  })

  it('does NOT render revoke button for CONSUMED token', () => {
    const { queryAllByText } = setup([CONSUMED_TOKEN])
    const revokeBtns = queryAllByText('table.revoke')
    expect(revokeBtns.length).toBe(0)
  })

  it('does NOT render revoke button for EXPIRED token', () => {
    const { queryAllByText } = setup([EXPIRED_TOKEN])
    const revokeBtns = queryAllByText('table.revoke')
    expect(revokeBtns.length).toBe(0)
  })

  it('does NOT render revoke button for REVOKED token', () => {
    const { queryAllByText } = setup([REVOKED_TOKEN])
    const revokeBtns = queryAllByText('table.revoke')
    expect(revokeBtns.length).toBe(0)
  })

  it('renders exactly one revoke button in a mixed list (1 ACTIVE + 1 CONSUMED)', () => {
    const { getAllByText } = setup([ACTIVE_TOKEN, CONSUMED_TOKEN])
    const revokeBtns = getAllByText('table.revoke')
    expect(revokeBtns.length).toBe(1)
    // Verify it's the ACTIVE token row
    const activeRow = getAllByText('web-01')[0].closest('tr')
    expect(within(activeRow!).getByText('table.revoke')).toBeDefined()
    // Verify CONSUMED row has no revoke
    const consumedRow = getAllByText('db-01')[0].closest('tr')
    expect(within(consumedRow!).queryByText('table.revoke')).toBeNull()
  })

  it('renders exactly one revoke button in a mixed list (1 ACTIVE + 1 EXPIRED + 1 REVOKED)', () => {
    const { getAllByText } = setup([ACTIVE_TOKEN, EXPIRED_TOKEN, REVOKED_TOKEN])
    const revokeBtns = getAllByText('table.revoke')
    expect(revokeBtns.length).toBe(1)
  })

  it('renders no revoke buttons when all tokens are non-ACTIVE', () => {
    const { queryAllByText } = setup([CONSUMED_TOKEN, EXPIRED_TOKEN, REVOKED_TOKEN])
    const revokeBtns = queryAllByText('table.revoke')
    expect(revokeBtns.length).toBe(0)
  })

  it('calls onRevoke when revoke button is clicked', () => {
    const { getByText } = setup([ACTIVE_TOKEN])
    const revokeBtn = getByText('table.revoke')
    revokeBtn.click()
    expect(DEFAULT_HANDLERS.onRevoke).toHaveBeenCalledWith(ACTIVE_TOKEN)
    expect(DEFAULT_HANDLERS.onRevoke).toHaveBeenCalledTimes(1)
  })

  it('disables revoke button when pendingRevokeId matches token id', () => {
    const { getByText } = setup([ACTIVE_TOKEN], false, ACTIVE_TOKEN.id)
    const revokeBtn = getByText('table.revoke').closest('button') as HTMLButtonElement
    expect(revokeBtn.disabled).toBe(true)
  })

  it('enables revoke button when pendingRevokeId does not match token id', () => {
    const { getByText } = setup([ACTIVE_TOKEN], false, 't-other')
    const revokeBtn = getByText('table.revoke').closest('button') as HTMLButtonElement
    expect(revokeBtn.disabled).toBe(false)
  })
})
