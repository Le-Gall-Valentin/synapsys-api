import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/features/auth', () => ({
  LoginForm: ({ labelId }: { labelId?: string }) => <form aria-label="login" aria-labelledby={labelId} />,
}))

describe('LoginPage', () => {
  beforeEach(() => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    )
  })

  it('renders the login form', () => {
    expect(screen.getByRole('form')).not.toBeNull()
  })

  it('renders the page title', () => {
    expect(screen.getByRole('heading', { level: 1 })).not.toBeNull()
    expect(screen.getByText('form.title')).not.toBeNull()
  })

  it('renders the subtitle', () => {
    expect(screen.getByText('form.subtitle')).not.toBeNull()
  })

  it('renders help text', () => {
    expect(screen.getByText('help.no_account')).not.toBeNull()
    expect(screen.getByText('help.contact_admin')).not.toBeNull()
  })

  it('renders the footer', () => {
    expect(screen.getByRole('contentinfo')).not.toBeNull()
    expect(screen.getByText('footer')).not.toBeNull()
  })

  it('links the form to the title via aria-labelledby', () => {
    const heading = screen.getByRole('heading', { level: 1 })
    const form = screen.getByRole('form')
    expect(heading.id).toBeTruthy()
    expect(form.getAttribute('aria-labelledby')).toBe(heading.id)
  })
})