type LogoutCallback = () => void

let logoutCallback: LogoutCallback | null = null

export function registerLogoutCallback(cb: LogoutCallback): void {
  logoutCallback = cb
}

export function triggerLogout(): void {
  logoutCallback?.()
}