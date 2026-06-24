export type EnrollmentTokenStatus = 'ACTIVE' | 'CONSUMED' | 'EXPIRED' | 'REVOKED'

export interface TokenCreator {
  id: string
  username: string
}

/** Enrollment token as listed by the backend (mirrors `EnrollmentTokenResponse`). The raw secret is never part of this shape. */
export interface EnrollmentToken {
  id: string
  serverName: string
  status: EnrollmentTokenStatus
  expiresAt: string
  createdBy: TokenCreator
  createdAt: string
}

/** Result of a creation (mirrors `CreatedTokenResponse`). `token` is the clear-text secret, returned only once. */
export interface CreatedToken {
  id: string
  serverName: string
  token: string
  status: EnrollmentTokenStatus
  expiresAt: string
  createdAt: string
}

/** Paginated tokens payload (mirrors `PageResponse`). */
export interface TokensPage {
  content: EnrollmentToken[]
  totalElements: number
  page: number
  size: number
}

/**
 * Port for enrollment-token management. Hooks depend on this contract, never on
 * the concrete axios implementation, which is injected through EnrollmentTokensApiProvider.
 */
export interface IEnrollmentTokensApi {
  listTokens(page: number, size?: number): Promise<TokensPage>
  createToken(serverName: string, ttlMinutes?: number): Promise<CreatedToken>
  revokeToken(id: string): Promise<void>
}
