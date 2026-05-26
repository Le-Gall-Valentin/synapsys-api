import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { describe, it, expect, vi } from 'vitest'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/features/auth', () => ({
  LoginForm: () => <form aria-label="login" />,
}))

describe('LoginPage', () => {
  it('renders the login form', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    )
    expect(screen.getByRole('form')).not.toBeNull()
  })
})