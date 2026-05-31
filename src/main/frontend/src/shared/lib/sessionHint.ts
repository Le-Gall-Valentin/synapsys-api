const SESSION_HINT_KEY = 'synapsys.hasSession'

export function setSessionHint(): void {
  try {
    sessionStorage.setItem(SESSION_HINT_KEY, '1')
  } catch {
    // sessionStorage unavailable (private mode, quota exceeded)
  }
}

export function clearSessionHint(): void {
  try {
    sessionStorage.removeItem(SESSION_HINT_KEY)
  } catch {
    // sessionStorage unavailable (private mode, quota exceeded)
  }
}

export function hasSessionHint(): boolean {
  try {
    return sessionStorage.getItem(SESSION_HINT_KEY) === '1'
  } catch {
    return false
  }
}
