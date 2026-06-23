package com.synapsys.api.agent.infrastructure.security;

import com.synapsys.api.agent.domain.port.out.AgentFingerprintPort;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class AgentFingerprintDeriver implements AgentFingerprintPort {

    @Override
    public String fingerprint(byte[] publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(publicKey));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
