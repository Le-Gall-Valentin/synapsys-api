import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import i18n from '../i18n'

export type Language = 'fr' | 'en'

interface LanguageContextValue {
  language: Language
  setLanguage: (lang: Language) => void
}

const LanguageContext = createContext<LanguageContextValue | null>(null)

function toLanguage(lng: string): Language {
  return lng.startsWith('fr') ? 'fr' : 'en'
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(() =>
    toLanguage(i18n.language)
  )

  useEffect(() => {
    const handler = (lng: string) => setLanguageState(toLanguage(lng))
    i18n.on('languageChanged', handler)
    return () => { i18n.off('languageChanged', handler) }
  }, [])

  function setLanguage(lang: Language): void {
    void i18n.changeLanguage(lang)
  }

  return (
    <LanguageContext.Provider value={{ language, setLanguage }}>
      {children}
    </LanguageContext.Provider>
  )
}

export function useLanguage(): LanguageContextValue {
  const ctx = useContext(LanguageContext)
  if (!ctx) throw new Error('useLanguage must be used inside LanguageProvider')
  return ctx
}