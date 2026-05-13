import type { InputHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
}

export function Input({ label, className = '', ...props }: InputProps) {
  const id = props.id ?? props.name ?? label.toLowerCase().replace(/\s+/g, '-')

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-xs font-medium text-fg-1">
        {label}
      </label>
      <input
        id={id}
        className={`w-full rounded-lg border border-border bg-bg-1 px-3.5 py-3 text-sm text-fg-0 outline-none placeholder:text-fg-3 transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_rgba(94,234,212,0.12)] ${className}`}
        {...props}
      />
    </div>
  )
}