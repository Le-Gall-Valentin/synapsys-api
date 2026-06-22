package com.synapsys.api.agent.domain.model;

public record Ed25519PublicKey(byte[] value) {
    private static final int ED25519_KEY_LENGTH = 32;

    public Ed25519PublicKey {
        if (value == null || value.length != ED25519_KEY_LENGTH) {
            throw new AgentException.InvalidPublicKey();
        }
        value = value.clone();
    }

    public byte[] value() {
        return value.clone();
    }
}
