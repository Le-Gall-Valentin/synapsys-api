import i18next from 'i18next'

type LocaleResources = Record<string, unknown>

/**
 * Self-registers translation resources for a given namespace.
 * Call this as a side-effect in each feature/page locales/index.ts.
 * Works before and after i18next.init() — resources are stored immediately.
 */
export function registerLocales(
  namespace: string,
  translations: { en: LocaleResources; fr: LocaleResources }
): void {
  if (typeof i18next?.addResourceBundle !== 'function') return
  i18next.addResourceBundle('en', namespace, translations.en, true, false)
  i18next.addResourceBundle('fr', namespace, translations.fr, true, false)
}