import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { CreateUserModal } from './CreateUserModal'
import { NetworkError, ServerError } from '@/shared/lib'
import { ConflictError } from '../api/adminUsersApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/shared/ui', () => ({
  Dialog: ({ children, open }: { children: React.ReactNode; open: boolean }) =>
    open ? <div data-testid="dialog">{children}</div> : null,
  Button: ({ children, onClick, disabled, isLoading, type, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean; children: React.ReactNode }) => (
    <button type={type} onClick={onClick} disabled={disabled || isLoading} {...props}>{children}</button>
  ),
  Input: ({ label, name, type = 'text', value, onChange, disabled, placeholder, autoFocus }: {
    label: string; name: string; type?: string; value: string;
    onChange: React.ChangeEventHandler<HTMLInputElement>; disabled?: boolean; placeholder?: string; autoFocus?: boolean
  }) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} type={type} value={value} onChange={onChange} disabled={disabled} placeholder={placeholder} autoFocus={autoFocus} />
    </div>
  ),
}))

function setup(overrides: { onCreate?: () => Promise<void> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onCreate = overrides.onCreate ?? vi.fn().mockResolvedValue(undefined)
  const result = render(
    <CreateUserModal onClose={onClose} onCreate={onCreate} onSuccess={onSuccess} />
  )
  return { ...result, onClose, onSuccess, onCreate }
}

function fillForm(getByLabelText: ReturnType<typeof render>['getByLabelText']) {
  fireEvent.change(getByLabelText('create.username'), { target: { value: 'bob' } })
  fireEvent.change(getByLabelText('create.email'), { target: { value: 'bob@test.com' } })
  fireEvent.change(getByLabelText('create.password'), { target: { value: 'P@ss1!' } })
}

beforeEach(() => { vi.clearAllMocks() })

describe('CreateUserModal', () => {
  it('submit button is disabled when fields are empty', () => {
    const { getByText } = setup()
    const btn = getByText('create.submit') as HTMLButtonElement
    expect(btn.disabled).toBe(true)
  })

  it('calls onCreate with trimmed values and selected role on submit', async () => {
    const { getByLabelText, getByText, onCreate, onSuccess } = setup()
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('bob', 'bob@test.com', 'P@ss1!', 'USER'))
    expect(onSuccess).toHaveBeenCalledOnce()
  })

  it('trims whitespace from username and email', async () => {
    const { getByLabelText, getByText, onCreate } = setup()
    fireEvent.change(getByLabelText('create.username'), { target: { value: '  bob  ' } })
    fireEvent.change(getByLabelText('create.email'), { target: { value: '  bob@test.com  ' } })
    fireEvent.change(getByLabelText('create.password'), { target: { value: 'P@ss1!' } })
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('bob', 'bob@test.com', 'P@ss1!', 'USER'))
  })

  it('shows conflict error on 409', async () => {
    const { getByLabelText, getByText, findByRole } = setup({
      onCreate: vi.fn().mockRejectedValue(new ConflictError()),
    })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('create.error.conflict')
  })

  it('shows server error', async () => {
    const { getByLabelText, getByText, findByRole } = setup({
      onCreate: vi.fn().mockRejectedValue(new ServerError()),
    })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('create.error.server')
  })

  it('shows network error', async () => {
    const { getByLabelText, getByText, findByRole } = setup({
      onCreate: vi.fn().mockRejectedValue(new NetworkError()),
    })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('create.error.network')
  })

  it('calls onClose when cancel is clicked', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('create.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('prevents double submit', async () => {
    let resolve!: () => void
    const onCreate = vi.fn().mockImplementation(() => new Promise<void>(r => { resolve = r }))
    const { getByLabelText, getByText } = setup({ onCreate })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    fireEvent.click(getByText('create.submit'))
    resolve()
    await waitFor(() => expect(onCreate).toHaveBeenCalledOnce())
  })
})
