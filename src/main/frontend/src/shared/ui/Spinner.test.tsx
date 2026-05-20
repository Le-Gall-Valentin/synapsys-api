import { render } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Spinner } from './Spinner'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

describe('Spinner', () => {
  it('renders with default label', () => {
    const { container } = render(<Spinner />)
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('renders with custom label', () => {
    const { container } = render(<Spinner label="Chargement..." />)
    const el = container.querySelector('[role="status"]')
    expect(el?.getAttribute('aria-label')).toBe('Chargement...')
  })
})