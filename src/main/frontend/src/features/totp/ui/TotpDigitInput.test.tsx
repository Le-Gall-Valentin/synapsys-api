import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { TotpDigitInput } from './TotpDigitInput'

function setup(value = '', onChange = vi.fn()) {
  return render(<TotpDigitInput value={value} onChange={onChange} />)
}

describe('TotpDigitInput', () => {
  it('renders 6 inputs', () => {
    const { getAllByRole } = setup()
    expect(getAllByRole('textbox')).toHaveLength(6)
  })

  it('shows value digits in correct positions', () => {
    const { getAllByRole } = setup('123')
    const inputs = getAllByRole('textbox') as HTMLInputElement[]
    expect(inputs[0].value).toBe('1')
    expect(inputs[1].value).toBe('2')
    expect(inputs[2].value).toBe('3')
    expect(inputs[3].value).toBe('')
    expect(inputs[4].value).toBe('')
    expect(inputs[5].value).toBe('')
  })

  it('calls onChange when a digit is typed', () => {
    const onChange = vi.fn()
    const { getAllByRole } = setup('', onChange)
    const inputs = getAllByRole('textbox')
    fireEvent.change(inputs[0], { target: { value: '5' } })
    expect(onChange).toHaveBeenCalledWith('5')
  })

  it('auto-advances focus to next input after digit entry', () => {
    const { getAllByRole } = setup('', vi.fn())
    const inputs = getAllByRole('textbox') as HTMLInputElement[]
    inputs[0].focus()
    fireEvent.change(inputs[0], { target: { value: '3' } })
    expect(document.activeElement).toBe(inputs[1])
  })

  it('backspace on empty input focuses previous and clears it', () => {
    const onChange = vi.fn()
    const { getAllByRole } = setup('12', onChange)
    const inputs = getAllByRole('textbox') as HTMLInputElement[]
    // index 2 is empty, pressing backspace should clear index 1 and focus it
    inputs[2].focus()
    fireEvent.keyDown(inputs[2], { key: 'Backspace' })
    expect(onChange).toHaveBeenCalledWith('1')
    expect(document.activeElement).toBe(inputs[1])
  })

  it('arrow left navigates to previous input', () => {
    const { getAllByRole } = setup('123456')
    const inputs = getAllByRole('textbox') as HTMLInputElement[]
    inputs[3].focus()
    fireEvent.keyDown(inputs[3], { key: 'ArrowLeft' })
    expect(document.activeElement).toBe(inputs[2])
  })

  it('arrow right navigates to next input', () => {
    const { getAllByRole } = setup('123456')
    const inputs = getAllByRole('textbox') as HTMLInputElement[]
    inputs[2].focus()
    fireEvent.keyDown(inputs[2], { key: 'ArrowRight' })
    expect(document.activeElement).toBe(inputs[3])
  })

  it('paste fills all 6 inputs from clipboard text', () => {
    const onChange = vi.fn()
    const { getByRole } = setup('', onChange)
    const container = getByRole('group')
    fireEvent.paste(container, {
      clipboardData: { getData: () => '123456' },
    })
    expect(onChange).toHaveBeenCalledWith('123456')
  })

  it('paste strips non-numeric characters', () => {
    const onChange = vi.fn()
    const { getByRole } = setup('', onChange)
    const container = getByRole('group')
    fireEvent.paste(container, {
      clipboardData: { getData: () => '1a2b3c4d5e6f' },
    })
    expect(onChange).toHaveBeenCalledWith('123456')
  })

  it('disabled inputs are disabled', () => {
    const { getAllByRole } = render(
      <TotpDigitInput value="" onChange={vi.fn()} disabled />
    )
    const inputs = getAllByRole('textbox') as HTMLInputElement[]
    inputs.forEach(input => expect(input.disabled).toBe(true))
  })
})