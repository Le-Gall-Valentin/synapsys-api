import { useTranslation } from 'react-i18next'
import type { User } from '@/entities/user'

const ROLE_PILL_CLASS: Record<User['role'], string> = {
  SUPER_ADMIN: 'border-accent/20 bg-accent-dim text-accent',
  ADMIN: 'border-border-2 bg-bg-3 text-fg-2',
  USER: 'border-status-blue/20 bg-status-blue-dim text-status-blue',
}

function getInitials(username: string): string {
  return username
    .split(/[.\s_-]/)
    .map(s => s[0] ?? '')
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

interface ProfileSummaryCardProps {
  user: User
}

export function ProfileSummaryCard({ user }: ProfileSummaryCardProps) {
  const { t, i18n } = useTranslation(['account', 'shell'])

  const createdDate = new Date(user.createdAt).toLocaleDateString(
    i18n.language === 'fr' ? 'fr-FR' : 'en-GB',
    { day: '2-digit', month: 'long', year: 'numeric' }
  )

  const avatarGradient =
    user.role === 'SUPER_ADMIN'
      ? 'linear-gradient(135deg, #a78bfa, #818cf8)'
      : 'linear-gradient(135deg, var(--color-accent), #818cf8)'

  return (
    <div className="rounded-md border border-border bg-bg-1 p-3.5 mb-4">
      <div className="flex items-center gap-4">
        <div
          className="w-[52px] h-[52px] rounded-[10px] flex items-center justify-center text-[18px] font-semibold shrink-0"
          style={{ background: avatarGradient, color: '#0a0b0d' }}
        >
          {getInitials(user.username)}
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-[18px] font-semibold text-fg-0 tracking-[-0.005em] truncate leading-snug">
            {user.username}
          </div>
          <div className="flex flex-wrap items-center gap-x-[10px] gap-y-1 mt-1.5">
            <span
              className={`inline-flex items-center px-[7px] py-[2px] rounded-full border text-[10px] font-semibold font-mono uppercase tracking-[0.04em] whitespace-nowrap ${ROLE_PILL_CLASS[user.role]}`}
            >
              {t(`user.role.${user.role}`, { ns: 'shell' })}
            </span>
            <span className="font-mono text-[11px] text-fg-3 truncate">{user.email}</span>
            <span className="font-mono text-[11px] text-fg-3">
              {t('member_since', { date: createdDate })}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}