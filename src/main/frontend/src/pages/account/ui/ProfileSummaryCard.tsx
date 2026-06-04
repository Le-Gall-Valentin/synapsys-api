import { useTranslation } from 'react-i18next'
import type { User } from '@/entities/user'

const ROLE_LABEL: Record<User['role'], string> = {
  SUPER_ADMIN: 'Super Admin',
  ADMIN: 'Admin',
  USER: 'Utilisateur',
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
  const { t, i18n } = useTranslation('account')

  const createdDate = new Date(user.createdAt).toLocaleDateString(
    i18n.language === 'fr' ? 'fr-FR' : 'en-GB',
    { day: '2-digit', month: 'long', year: 'numeric' }
  )

  const avatarBg = user.role === 'SUPER_ADMIN'
    ? 'bg-gradient-to-br from-purple-400 to-indigo-400'
    : 'bg-bg-3'

  return (
    <div className="rounded-xl border border-border bg-bg-1 p-4 mb-4">
      <div className="flex items-center gap-4">
        <div className={`size-14 rounded-full ${avatarBg} flex items-center justify-center text-lg font-semibold text-fg-0 shrink-0`}>
          {getInitials(user.username)}
        </div>
        <div className="flex-1 min-w-0">
          <div className="font-semibold text-fg-0 text-base truncate">{user.username}</div>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-1">
            <span className="rounded-full border border-border px-2 py-0.5 text-xs font-medium text-fg-1">
              {ROLE_LABEL[user.role]}
            </span>
            <span className="text-xs text-fg-2 truncate">{user.email}</span>
            <span className="text-xs text-fg-2">{t('member_since', { date: createdDate })}</span>
          </div>
        </div>
      </div>
    </div>
  )
}