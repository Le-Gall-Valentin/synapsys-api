import { useState, type FormEvent } from 'react'
import { AlertTriangle, Eye, EyeOff, ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../model/useAuth'

export function LoginForm() {
  const { login } = useAuth()
  const { t } = useTranslation('auth')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [hasError, setHasError] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    setHasError(false)
    setIsLoading(true)
    try {
      await login({ username, password })
    } catch {
      setHasError(true)
      setPassword('')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
      {hasError && (
        <div className="flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red">
          <AlertTriangle className="size-3.5 shrink-0" />
          {t('error.credentials')}
        </div>
      )}

      <div className="flex flex-col gap-2">
        <label htmlFor="username" className="text-xs font-medium text-fg-1">
          {t('field.username')}
        </label>
        <input
          id="username"
          name="username"
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder={t('field.username_placeholder')}
          required
          autoComplete="username"
          autoFocus
          className="w-full rounded-lg border border-border bg-bg-1 px-3.5 py-3 text-sm text-fg-0 outline-none placeholder:text-fg-3 transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_rgba(94,234,212,0.12)]"
        />
      </div>

      <div className="flex flex-col gap-2">
        <label htmlFor="password" className="text-xs font-medium text-fg-1">
          {t('field.password')}
        </label>
        <div className="relative">
          <input
            id="password"
            name="password"
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder={t('field.password_placeholder')}
            required
            autoComplete="current-password"
            className="w-full rounded-lg border border-border bg-bg-1 py-3 pl-3.5 pr-11 text-sm text-fg-0 outline-none placeholder:text-fg-3 transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_rgba(94,234,212,0.12)]"
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-1.5 top-1/2 -translate-y-1/2 rounded-md p-2 text-fg-3 transition-colors hover:bg-bg-2 hover:text-fg-0"
            aria-label={showPassword ? t('field.hide_password') : t('field.show_password')}
          >
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
      </div>

      <button
        type="submit"
        disabled={isLoading}
        className="mt-2 flex items-center justify-center gap-1.5 rounded-lg py-3 text-sm font-semibold transition-transform active:translate-y-px disabled:cursor-wait disabled:opacity-70"
        style={{
          background: 'linear-gradient(180deg, #6dead0 0%, #4dd9c2 100%)',
          color: '#07211c',
          boxShadow:
            '0 1px 0 rgba(255,255,255,0.2) inset, 0 6px 16px rgba(94,234,212,0.15)',
        }}
      >
        {isLoading ? (
          <span
            className="size-4 animate-spin rounded-full border-2"
            style={{
              borderColor: 'rgba(7,33,28,0.25)',
              borderTopColor: '#07211c',
            }}
          />
        ) : (
          <>
            {t('action.submit')}
            <ChevronRight className="size-3.5" />
          </>
        )}
      </button>
    </form>
  )
}