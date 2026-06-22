package com.synapsys.api.agent.domain.port.out;

public interface AgentSignatureVerifierPort {
    boolean verify(byte[] publicKey, byte[] message, byte[] signature);
}
