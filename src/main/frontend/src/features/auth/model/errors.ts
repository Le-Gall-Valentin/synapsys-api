export class CredentialsError extends Error {
  constructor() { super('Invalid credentials') }
}

export class NetworkError extends Error {
  constructor() { super('Network error') }
}

export class ServerError extends Error {
  constructor() { super('Server error') }
}

export class RateLimitError extends Error {
  constructor() { super('Too many requests') }
}