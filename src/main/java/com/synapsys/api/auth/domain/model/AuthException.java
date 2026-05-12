package com.synapsys.api.auth.domain.model;

public abstract sealed class AuthException extends RuntimeException
    permits AuthException.InvalidCredentials,
            AuthException.UserNotActive,
            AuthException.TokenExpired,
            AuthException.TokenRevoked {

    private AuthException(String message) {
        super(message);
    }

    public static final class InvalidCredentials extends AuthException {
        public InvalidCredentials() { super("Invalid credentials"); }
    }

    public static final class UserNotActive extends AuthException {
        public UserNotActive() { super("User account is disabled"); }
    }

    public static final class TokenExpired extends AuthException {
        public TokenExpired() { super("Token has expired or was not found"); }
    }

    public static final class TokenRevoked extends AuthException {
        public TokenRevoked() { super("Token has been revoked"); }
    }
}