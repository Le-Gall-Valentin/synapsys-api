type LogoutCallback = () => void
type NavigateCallback = (to: string) => void

let logoutCallback: LogoutCallback | null = null
let navigateCallback: NavigateCallback | null = null
let authenticated = false

export function registerLogoutCallback(cb: LogoutCallback): void {
  logoutCallback = cb
}

export function registerNavigateCallback(cb: NavigateCallback): void {
  navigateCallback = cb
}

export function triggerLogout(): void {
  logoutCallback?.()
}

export function triggerNavigate(to: string): void {
  navigateCallback?.(to)
}

export function setAuthState(value: boolean): void {
  authenticated = value
}

export function isAuthenticatedState(): boolean {
  return authenticated
}