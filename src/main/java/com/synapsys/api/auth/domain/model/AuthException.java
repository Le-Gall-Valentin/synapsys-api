package com.synapsys.api.auth.domain.model;

public abstract sealed class AuthException extends RuntimeException
    permits AuthException.InvalidCredentials,
            AuthException.UserNotActive,
            AuthException.UserAlreadyInactive,
            AuthException.TokenExpired,
            AuthException.TokenNotFound,
            AuthException.TokenRevoked,
            AuthException.UserNotFound,
            AuthException.UsernameAlreadyExists,
            AuthException.EmailAlreadyExists,
            AuthException.InsufficientPermissions,
            AuthException.DataIntegrityError,
            AuthException.TotpCodeInvalid,
            AuthException.TotpAlreadyEnabled,
            AuthException.TotpNotEnabled,
            AuthException.TotpSetupNotStarted,
            AuthException.TotpChallengeExpired,
            AuthException.TotpMaxAttemptsExceeded {

    private AuthException(String message) {
        super(message);
    }

    public static final class InvalidCredentials extends AuthException {
        public InvalidCredentials() { super("Invalid credentials"); }
    }

    public static final class UserNotActive extends AuthException {
        public UserNotActive() { super("User account is disabled"); }
    }

    public static final class UserAlreadyInactive extends AuthException {
        public UserAlreadyInactive() { super("User account is already inactive"); }
    }

    public static final class TokenExpired extends AuthException {
        public TokenExpired() { super("Token has expired"); }
    }

    public static final class TokenNotFound extends AuthException {
        public TokenNotFound() { super("Token not found"); }
    }

    public static final class TokenRevoked extends AuthException {
        public TokenRevoked() { super("Token has been revoked"); }
    }

    public static final class UserNotFound extends AuthException {
        public UserNotFound() { super("User not found"); }
    }

    public static final class UsernameAlreadyExists extends AuthException {
        public UsernameAlreadyExists() { super("Username already taken"); }
    }

    public static final class EmailAlreadyExists extends AuthException {
        public EmailAlreadyExists() { super("Email already taken"); }
    }

    public static final class InsufficientPermissions extends AuthException {
        public InsufficientPermissions() {
            super("Insufficient permissions");
        }
    }

    public static final class DataIntegrityError extends AuthException {
        public DataIntegrityError() {
            super("Unexpected data integrity violation");
        }
    }

    public static final class TotpCodeInvalid extends AuthException {
        public TotpCodeInvalid() { super("Invalid or expired TOTP code"); }
    }

    public static final class TotpAlreadyEnabled extends AuthException {
        public TotpAlreadyEnabled() { super("Two-factor authentication is already enabled"); }
    }

    public static final class TotpNotEnabled extends AuthException {
        public TotpNotEnabled() { super("Two-factor authentication is not enabled"); }
    }

    public static final class TotpSetupNotStarted extends AuthException {
        public TotpSetupNotStarted() { super("Two-factor authentication setup has not been started"); }
    }

    public static final class TotpChallengeExpired extends AuthException {
        public TotpChallengeExpired() { super("Two-factor authentication challenge has expired"); }
    }

    public static final class TotpMaxAttemptsExceeded extends AuthException {
        public TotpMaxAttemptsExceeded() { super("Too many incorrect TOTP attempts"); }
    }
}
