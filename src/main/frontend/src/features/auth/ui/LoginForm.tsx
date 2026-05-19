import { isAxiosError } from 'axios'
import { useState, type FormEvent } from 'react'
import { AlertTriangle, Eye, EyeOff, ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'
import { Button, Input } from '@/shared/ui'

type ErrorKind = 'credentials' | 'network' | null

export function LoginForm() {
  const login = useAuth((s) => s.login)
  const { t } = useTranslation('auth')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errorKind, setErrorKind] = useState<ErrorKind>(null)
  const [isLoading, setIsLoading] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    setErrorKind(null)
    setIsLoading(true)
    try {
      await login({ username, password })
    } catch (error) {
      const is401 = isAxiosError(error) && error.response?.status === 401
      if (is401) {
        setErrorKind('credentials')
        setPassword('')
      } else {
        setErrorKind('network')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {errorKind && (
        <div
          role="alert"
          className="flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red"
        >
          <AlertTriangle className="size-3.5 shrink-0" />
          {t(`error.${errorKind}`)}
        </div>
      )}

      <Input
        label={t('field.username')}
        name="username"
        type="text"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder={t('field.username_placeholder')}
        required
        autoComplete="username"
        autoFocus
      />

      <Input
        label={t('field.password')}
        name="password"
        type={showPassword ? 'text' : 'password'}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder={t('field.password_placeholder')}
        required
        autoComplete="current-password"
        suffix={
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="rounded-md p-2 text-fg-3 transition-colors hover:bg-bg-2 hover:text-fg-0"
            aria-label={showPassword ? t('field.hide_password') : t('field.show_password')}
          >
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        }
      />

      <Button
        type="submit"
        isLoading={isLoading}
        className="mt-2 w-full border-transparent py-3 font-semibold active:translate-y-px disabled:cursor-wait"
        style={{
          background: 'linear-gradient(180deg, #6dead0 0%, #4dd9c2 100%)',
          color: '#07211c',
          boxShadow:
            '0 1px 0 rgba(255,255,255,0.2) inset, 0 6px 16px rgba(94,234,212,0.15)',
        }}
      >
        {t('action.submit')}
        <ChevronRight className="size-3.5" />
      </Button>
    </form>
  )
}