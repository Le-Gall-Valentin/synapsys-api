export class TotpCodeError extends Error {
  constructor() { super('Invalid TOTP code'); this.name = 'TotpCodeError' }
}

export class TotpChallengeExpiredError extends Error {
  constructor() { super('TOTP challenge expired'); this.name = 'TotpChallengeExpiredError' }
}

export class TotpAlreadyEnabledError extends Error {
  constructor() { super('TOTP is already enabled'); this.name = 'TotpAlreadyEnabledError' }
}