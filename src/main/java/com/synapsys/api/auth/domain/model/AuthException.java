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
            AuthException.DataIntegrityError {

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
        public DataIntegrityError(String constraint) {
            super(constraint != null
                ? "Unexpected data integrity violation on constraint: " + constraint
                : "Unexpected data integrity violation");
        }
    }
}
