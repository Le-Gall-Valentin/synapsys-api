// Each new page must be manually registered here (translations are not auto-discovered).
// This is an intentional simplicity trade-off for MVP. Future: replace with i18next
// lazy-loading by namespace so pages self-register without touching this file.
import { commonTranslations } from '@/shared/config'
import { authTranslations } from '@/features/auth'
import { totpTranslations } from '@/features/totp'
import { loginTranslations } from '@/pages/login'
import { profileTranslations } from '@/pages/profile'

export const i18nResources = {
  en: {
    common: commonTranslations.en,
    auth: authTranslations.en,
    totp: totpTranslations.en,
    login: loginTranslations.en,
    profile: profileTranslations.en,
  },
  fr: {
    common: commonTranslations.fr,
    auth: authTranslations.fr,
    totp: totpTranslations.fr,
    login: loginTranslations.fr,
    profile: profileTranslations.fr,
  },
}