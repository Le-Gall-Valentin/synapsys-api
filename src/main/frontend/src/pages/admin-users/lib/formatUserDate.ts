/** Formats a user's creation date for the supported UI languages. */
export function formatUserDate(createdAt: string, lang: string): string {
  return new Date(createdAt).toLocaleDateString(lang === 'fr' ? 'fr-FR' : 'en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}
