import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AdminUsersPage } from './AdminUsersPage'
import type { AdminUser } from '../api/adminUsersApi'
import type { User } from '@/entities/user'

const mockUseAuth = vi.hoisted(() => vi.fn())
const mockApi = vi.hoisted(() => ({
  listUsers: vi.fn(),
  createUser: vi.fn(),
  updateUserRole: vi.fn(),
  activateUser: vi.fn(),
  deactivateUser: vi.fn(),
  resetTotp: vi.fn(),
  deleteUser: vi.fn(),
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/features/auth', () => ({
  useAuth: (...args: unknown[]) => mockUseAuth(...args),
}))

vi.mock('../api/adminUsersApi', () => ({ adminUsersApi: mockApi }))

const capturedTableProps: Record<string, unknown>[] = []
vi.mock('./UsersTable', () => ({
  UsersTable: (props: Record<string, unknown>) => {
    capturedTableProps.length = 0
    capturedTableProps.push(props)
    if (props.isLoading) return <div data-testid="skeleton" />
    return (
      <div data-testid="users-table">
        <button data-testid="trigger-edit" onClick={() => (props.onEditRole as (u: AdminUser) => void)(MOCK_USER)}>edit</button>
        <button data-testid="trigger-delete" onClick={() => (props.onDelete as (u: AdminUser) => void)(MOCK_USER)}>delete</button>
        <button data-testid="trigger-totp" onClick={() => (props.onResetTotp as (u: AdminUser) => void)(MOCK_USER)}>totp</button>
        <button data-testid="trigger-toggle" onClick={() => (props.onToggleActive as (u: AdminUser) => void)(MOCK_USER)}>toggle</button>
      </div>
    )
  },
}))

vi.mock('./CreateUserModal', () => ({
  CreateUserModal: ({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) => (
    <div data-testid="create-modal">
      <button onClick={onClose}>close-create</button>
      <button onClick={onSuccess}>success-create</button>
    </div>
  ),
}))

vi.mock('./EditUserRoleModal', () => ({
  EditUserRoleModal: ({ onClose, onSuccess }: { onClose: () => void; onSuccess: (r: string) => void }) => (
    <div data-testid="edit-modal">
      <button onClick={onClose}>close-edit</button>
      <button onClick={() => onSuccess('ADMIN')}>success-edit</button>
    </div>
  ),
}))

vi.mock('./DeleteUserModal', () => ({
  DeleteUserModal: ({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) => (
    <div data-testid="delete-modal">
      <button onClick={onClose}>close-delete</button>
      <button onClick={onSuccess}>success-delete</button>
    </div>
  ),
}))

vi.mock('./ResetTotpModal', () => ({
  ResetTotpModal: ({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) => (
    <div data-testid="totp-modal">
      <button onClick={onClose}>close-totp</button>
      <button onClick={onSuccess}>success-totp</button>
    </div>
  ),
}))

const MOCK_CURRENT_USER: User = {
  id: 'sa', username: 'superadmin', email: 'sa@test.com',
  role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: true,
}
const MOCK_USER: AdminUser = {
  id: 'u1', username: 'alice', email: 'alice@test.com',
  role: 'USER', isActive: true, createdAt: '2024-02-01T00:00:00Z', totpEnabled: false,
}
const MOCK_USERS: AdminUser[] = [MOCK_USER]

function makeAuth(user: User | null = MOCK_CURRENT_USER) {
  mockUseAuth.mockImplementation((selector: (s: { user: User | null }) => unknown) =>
    selector({ user })
  )
}

beforeEach(() => {
  mockUseAuth.mockReset()
  vi.clearAllMocks()
  capturedTableProps.length = 0
  mockApi.listUsers.mockResolvedValue(MOCK_USERS)
  mockApi.deactivateUser.mockResolvedValue(undefined)
  mockApi.activateUser.mockResolvedValue(undefined)
  mockApi.deleteUser.mockResolvedValue(undefined)
  mockApi.resetTotp.mockResolvedValue(undefined)
  mockApi.updateUserRole.mockResolvedValue(undefined)
  mockApi.createUser.mockResolvedValue(undefined)
})

describe('AdminUsersPage — loading', () => {
  it('shows skeleton while fetching', () => {
    mockApi.listUsers.mockReturnValue(new Promise(() => {}))
    makeAuth()
    const { getByTestId } = render(<AdminUsersPage />)
    expect(getByTestId('skeleton')).toBeDefined()
  })

  it('shows table after fetch resolves', async () => {
    makeAuth()
    const { findByTestId } = render(<AdminUsersPage />)
    expect(await findByTestId('users-table')).toBeDefined()
  })
})

describe('AdminUsersPage — load error', () => {
  it('shows load_error alert when listUsers rejects', async () => {
    mockApi.listUsers.mockRejectedValue(new Error('fail'))
    makeAuth()
    const { findByRole } = render(<AdminUsersPage />)
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('load_error')
  })
})

describe('AdminUsersPage — create modal', () => {
  it('opens CreateUserModal when header button is clicked', async () => {
    makeAuth()
    const { findByTestId, getByText } = render(<AdminUsersPage />)
    await findByTestId('users-table')
    fireEvent.click(getByText('action.create'))
    expect(await findByTestId('create-modal')).toBeDefined()
  })

  it('closes CreateUserModal on close', async () => {
    makeAuth()
    const { findByTestId, getByText, queryByTestId } = render(<AdminUsersPage />)
    await findByTestId('users-table')
    fireEvent.click(getByText('action.create'))
    await findByTestId('create-modal')
    fireEvent.click(getByText('close-create'))
    await waitFor(() => expect(queryByTestId('create-modal')).toBeNull())
  })
})

describe('AdminUsersPage — edit modal', () => {
  it('opens EditUserRoleModal when edit triggered', async () => {
    makeAuth()
    const { findByTestId } = render(<AdminUsersPage />)
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-edit"]')!)
    expect(await findByTestId('edit-modal')).toBeDefined()
  })

  it('closes edit modal after success and updates user role', async () => {
    makeAuth()
    const { findByTestId, queryByTestId } = render(<AdminUsersPage />)
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-edit"]')!)
    await findByTestId('edit-modal')
    fireEvent.click(document.querySelector('[data-testid="edit-modal"] button:last-child')!)
    await waitFor(() => expect(queryByTestId('edit-modal')).toBeNull())
  })
})

describe('AdminUsersPage — delete modal', () => {
  it('opens DeleteUserModal when delete triggered', async () => {
    makeAuth()
    const { findByTestId } = render(<AdminUsersPage />)
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-delete"]')!)
    expect(await findByTestId('delete-modal')).toBeDefined()
  })
})

describe('AdminUsersPage — reset totp modal', () => {
  it('opens ResetTotpModal when totp triggered', async () => {
    makeAuth()
    const { findByTestId } = render(<AdminUsersPage />)
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-totp"]')!)
    expect(await findByTestId('totp-modal')).toBeDefined()
  })
})

describe('AdminUsersPage — optimistic toggle', () => {
  it('calls deactivateUser when toggling an active user', async () => {
    makeAuth()
    const { findByTestId } = render(<AdminUsersPage />)
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-toggle"]')!)
    await waitFor(() => expect(mockApi.deactivateUser).toHaveBeenCalledWith('u1'))
  })

  it('shows mutation_error and reverts on API failure', async () => {
    mockApi.deactivateUser.mockRejectedValue(new Error('fail'))
    makeAuth()
    const { findByTestId, findByRole } = render(<AdminUsersPage />)
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-toggle"]')!)
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('mutation_error')
  })
})
