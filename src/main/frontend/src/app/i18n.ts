import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

import commonEn from '@/shared/config/locales/en.json'
import commonFr from '@/shared/config/locales/fr.json'
import { authTranslations } from '@/features/auth/locales'
import { loginTranslations } from '@/pages/login/locales'
import { profileTranslations } from '@/pages/profile/locales'

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    supportedLngs: ['en', 'fr'],
    load: 'languageOnly',
    interpolation: { escapeValue: false },
    resources: {
      en: {
        common: commonEn,
        auth: authTranslations.en,
        login: loginTranslations.en,
        profile: profileTranslations.en,
      },
      fr: {
        common: commonFr,
        auth: authTranslations.fr,
        login: loginTranslations.fr,
        profile: profileTranslations.fr,
      },
    },
  })

export default i18n