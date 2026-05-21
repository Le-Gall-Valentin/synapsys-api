import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { i18nResources } from './i18n/resources'

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    supportedLngs: ['en', 'fr'],
    load: 'languageOnly',
    interpolation: { escapeValue: false },
    resources: i18nResources,
  })
  .then(() => {
    document.documentElement.lang = i18n.language
    i18n.on('languageChanged', (lng) => { document.documentElement.lang = lng })
  })

export default i18n