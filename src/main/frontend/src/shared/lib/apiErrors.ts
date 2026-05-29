export class NetworkError extends Error {
  constructor() { super('Network error'); this.name = 'NetworkError' }
}

export class ServerError extends Error {
  constructor() { super('Server error'); this.name = 'ServerError' }
}

export class RateLimitError extends Error {
  readonly retryAfterSeconds: number | null
  constructor(retryAfterSeconds: number | null = null) {
    super('Too many requests')
    this.name = 'RateLimitError'
    this.retryAfterSeconds = retryAfterSeconds
  }
}