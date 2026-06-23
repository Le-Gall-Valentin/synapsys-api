package com.synapsys.api.agent.domain.model;

public abstract sealed class AgentException extends RuntimeException
    permits AgentException.TokenNotFound,
            AgentException.TokenNotRevocable,
            AgentException.InvalidPublicKey,
            AgentException.PublicKeyAlreadyRegistered,
            AgentException.ServerNameInUse,
            AgentException.AgentNotFound,
            AgentException.AgentNotRevocable,
            AgentException.AgentNotDeletable,
            AgentException.HandshakeFailed,
            AgentException.DataIntegrityError,
            AgentException.EnrollmentRejected {

    private AgentException(String message) { super(message); }

    public static final class TokenNotFound extends AgentException {
        public TokenNotFound() { super("Enrollment token not found"); }
    }
    public static final class TokenNotRevocable extends AgentException {
        public TokenNotRevocable() { super("Enrollment token cannot be revoked"); }
    }
    public static final class InvalidPublicKey extends AgentException {
        public InvalidPublicKey() { super("Public key is not a valid Ed25519 key"); }
    }
    public static final class PublicKeyAlreadyRegistered extends AgentException {
        public PublicKeyAlreadyRegistered() { super("Public key is already registered"); }
    }
    public static final class ServerNameInUse extends AgentException {
        public ServerNameInUse() { super("Server name is already in use by a non-revoked agent"); }
    }
    public static final class AgentNotFound extends AgentException {
        public AgentNotFound() { super("Agent not found"); }
    }
    public static final class AgentNotRevocable extends AgentException {
        public AgentNotRevocable() { super("Agent cannot be revoked"); }
    }
    public static final class AgentNotDeletable extends AgentException {
        public AgentNotDeletable() { super("Agent must be revoked before deletion"); }
    }
    public static final class HandshakeFailed extends AgentException {
        public HandshakeFailed() { super("Agent handshake failed"); }
    }
    public static final class DataIntegrityError extends AgentException {
        public DataIntegrityError() { super("Unexpected data integrity violation"); }
    }
    public static final class EnrollmentRejected extends AgentException {
        public EnrollmentRejected() { super("Enrollment rejected"); }
    }
}

