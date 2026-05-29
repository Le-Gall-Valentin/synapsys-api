import React, { useRef, useState } from 'react'
import { AlertTriangle, Check, Info } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui'
import { TotpDigitInput } from './TotpDigitInput'
import { TotpChallengeExpiredError } from '../model/errors'
import type { ITotpApi } from '../api/ITotpApi'
import type { User } from '@/entities/user'

interface TotpVerifyStepProps {
  username: string
  api: ITotpApi
  onVerified: (user: User) => void
  onBack: () => void
}

export function TotpVerifyStep({ username, api, onVerified, onBack }: TotpVerifyStepProps) {
  const { t } = useTranslation('totp')
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const isSubmittingRef = useRef(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (code.length < 6) { setErrorKey('verify.error.incomplete'); return }
    if (isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      const user = await api.verify(code)
      onVerified(user)
    } catch (error) {
      if (error instanceof TotpChallengeExpiredError) {
        setErrorKey('verify.error.challenge_expired')
      } else {
        setErrorKey('verify.error.invalid_code')
        setCode('')
      }
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  return (
    <div>
      <div className="mb-8">
        <h2 className="mb-2 text-[28px] font-semibold tracking-tight text-fg-0">
          {t('verify.title')}
        </h2>
        <p className="text-sm text-fg-2">
          {t('verify.subtitle', { username })}
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <TotpDigitInput value={code} onChange={setCode} disabled={isLoading} autoFocus />

        {errorKey && (
          <div
            role="alert"
            className="flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red mb-3"
          >
            <AlertTriangle className="size-3.5 shrink-0" />
            {t(errorKey)}
          </div>
        )}

        <Button
          type="submit"
          isLoading={isLoading}
          className="mt-2 w-full border-transparent py-3 font-semibold active:translate-y-px disabled:cursor-wait"
          style={{ background: 'linear-gradient(180deg, #6dead0 0%, #4dd9c2 100%)', color: '#07211c' }}
        >
          <Check className="size-3.5" />
          {t('verify.submit')}
        </Button>

        <button
          type="button"
          onClick={onBack}
          className="w-full mt-2 bg-transparent border-0 text-fg-2 text-xs cursor-pointer py-1.5 hover:text-fg-0 text-center"
        >
          {t('verify.back')}
        </button>
      </form>

      <div className="mt-7 flex gap-2 items-start text-[13px] text-fg-2">
        <Info className="size-3.5 shrink-0 mt-0.5" />
        <span>{t('verify.help')}</span>
      </div>
    </div>
  )
}