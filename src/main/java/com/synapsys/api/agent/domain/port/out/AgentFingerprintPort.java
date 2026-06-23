package com.synapsys.api.agent.domain.port.out;

public interface AgentFingerprintPort {
    String fingerprint(byte[] publicKey);
}
