package com.synapsys.api.agent.domain.model;

public abstract sealed class AgentException extends RuntimeException
    permits AgentException.TokenNotConsumable,
            AgentException.TokenNotRevocable {

    private AgentException(String message) { super(message); }

    public static final class TokenNotConsumable extends AgentException {
        public TokenNotConsumable() { super("Enrollment token is not consumable"); }
    }
    public static final class TokenNotRevocable extends AgentException {
        public TokenNotRevocable() { super("Enrollment token cannot be revoked"); }
    }
}