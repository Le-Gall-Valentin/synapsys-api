import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { UsersTable } from './UsersTable'
import type { AdminUser } from '../api/adminUsersApi'
import type { User } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, string>) => {
    if (opts?.username) return `${k}:${opts.username}`
    return k
  }, i18n: { language: 'fr' } }),
}))

vi.mock('@/entities/user', async (importActual) => {
  const actual = await importActual<typeof import('@/entities/user')>()
  return { ...actual }
})

const SA: User = { id: 'sa', username: 'superadmin', email: 'sa@test.com', role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: true }
const ADMIN: User = { id: 'a1', username: 'adminuser', email: 'admin@test.com', role: 'ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false }

const USERS: AdminUser[] = [
  { ...SA, isActive: true },
  { ...ADMIN, isActive: true },
  { id: 'u1', username: 'testuser', email: 'test@test.com', role: 'USER', isActive: true, createdAt: '2024-02-01T00:00:00Z', totpEnabled: true },
  { id: 'u2', username: 'inactive', email: 'inactive@test.com', role: 'USER', isActive: false, createdAt: '2024-03-01T00:00:00Z', totpEnabled: false },
]

const DEFAULT_HANDLERS = {
  onToggleActive: vi.fn(),
  onEditRole: vi.fn(),
  onResetTotp: vi.fn(),
  onDelete: vi.fn(),
}

function setup(currentUser: User = SA, users: AdminUser[] = USERS, isLoading = false) {
  return render(
    <UsersTable
      users={users}
      isLoading={isLoading}
      currentUser={currentUser}
      {...DEFAULT_HANDLERS}
    />
  )
}

beforeEach(() => { vi.clearAllMocks() })

describe('UsersTable — loading', () => {
  it('renders skeleton rows when isLoading', () => {
    const { container } = setup(SA, [], true)
    const skeletonRows = container.querySelectorAll('.animate-pulse')
    expect(skeletonRows.length).toBeGreaterThan(0)
  })

  it('does not render table when isLoading', () => {
    const { queryByRole } = setup(SA, [], true)
    expect(queryByRole('table')).toBeNull()
  })
})

describe('UsersTable — rows', () => {
  it('renders a row for each user', () => {
    const { getAllByRole } = setup()
    const rows = getAllByRole('row')
    expect(rows.length).toBe(USERS.length + 1) // +1 for header
  })

  it('shows "vous" badge for the current user', () => {
    const { getByText } = setup(SA)
    expect(getByText('table.you')).toBeDefined()
  })

  it('shows totp_on pill for user with totp enabled', () => {
    const { getAllByText } = setup()
    expect(getAllByText('table.totp_on').length).toBeGreaterThan(0)
  })

  it('shows totp_off pill for user without totp', () => {
    const { getAllByText } = setup()
    expect(getAllByText('table.totp_off').length).toBeGreaterThan(0)
  })
})

describe('UsersTable — button permissions', () => {
  it('delete button is disabled for SUPER_ADMIN target', () => {
    const { getAllByRole } = setup(SA)
    const rows = getAllByRole('row').slice(1) // skip header
    const saRow = rows[0]
    const deleteBtn = saRow.querySelector('[aria-label="table.btn_delete"]') as HTMLButtonElement
    expect(deleteBtn?.disabled).toBe(true)
  })

  it('delete button is enabled for USER target when caller is SUPER_ADMIN', () => {
    const { getAllByRole } = setup(SA)
    const rows = getAllByRole('row').slice(1)
    const userRow = rows[2] // testuser
    const deleteBtn = userRow.querySelector('[aria-label^="table.btn_delete"]') as HTMLButtonElement
    expect(deleteBtn?.disabled).toBe(false)
  })

  it('edit button is disabled for SUPER_ADMIN target', () => {
    const { getAllByRole } = setup(SA)
    const rows = getAllByRole('row').slice(1)
    const saRow = rows[0]
    const editBtn = saRow.querySelector('[aria-label^="table.btn_edit"]') as HTMLButtonElement
    expect(editBtn?.disabled).toBe(true)
  })

  it('totp reset button is disabled if totp not enabled', () => {
    const { getAllByRole } = setup(SA)
    const rows = getAllByRole('row').slice(1)
    const adminRow = rows[1] // adminuser, totpEnabled: false
    const totpBtn = adminRow.querySelector('[aria-label^="table.btn_reset_totp"]') as HTMLButtonElement
    expect(totpBtn?.disabled).toBe(true)
  })

  it('ADMIN caller cannot edit ADMIN target', () => {
    const { getAllByRole } = setup(ADMIN)
    const rows = getAllByRole('row').slice(1)
    const saRow = rows[0] // SUPER_ADMIN
    const editBtn = saRow.querySelector('[aria-label^="table.btn_edit"]') as HTMLButtonElement
    expect(editBtn?.disabled).toBe(true)
  })
})
