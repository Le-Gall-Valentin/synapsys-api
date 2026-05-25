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
    ns: ['common', 'auth', 'login', 'profile'],
    defaultNS: 'common',
    load: 'languageOnly',
    interpolation: { escapeValue: false },
    resources: i18nResources,
  })
  .then(() => {
    document.documentElement.lang = i18n.language
    const onLanguageChanged = (lng: string) => { document.documentElement.lang = lng }
    i18n.on('languageChanged', onLanguageChanged)
    if (import.meta.hot) {
      import.meta.hot.dispose(() => { i18n.off('languageChanged', onLanguageChanged) })
    }
  })

export default i18n