package com.synapsys.api.agent.domain.model;

public abstract sealed class AgentException extends RuntimeException
    permits AgentException.TokenNotConsumable,
            AgentException.TokenNotRevocable,
            AgentException.AgentNotRevocable,
            AgentException.AgentNotDeletable,
            AgentException.HandshakeFailed,
            AgentException.InvalidPublicKey {

    private AgentException(String message) { super(message); }

    public static final class TokenNotConsumable extends AgentException {
        public TokenNotConsumable() { super("Enrollment token is not consumable"); }
    }
    public static final class TokenNotRevocable extends AgentException {
        public TokenNotRevocable() { super("Enrollment token cannot be revoked"); }
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
    public static final class InvalidPublicKey extends AgentException {
        public InvalidPublicKey() { super("Public key is not a valid Ed25519 key"); }
    }
}

