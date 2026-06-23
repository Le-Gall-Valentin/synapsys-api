package com.synapsys.api.agent.infrastructure.security;

import com.synapsys.api.agent.domain.port.out.AgentSignatureVerifierPort;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;

@Component
public class Ed25519SignatureVerifier implements AgentSignatureVerifierPort {

    @Override
    public boolean verify(byte[] publicKey, byte[] message, byte[] signature) {
        try {
            PublicKey pub = decodeRawPublicKey(publicKey);
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(pub);
            verifier.update(message);
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Any malformed key / signature counts as a failed verification, never an error to the caller.
            return false;
        }
    }

    /** Reconstructs an Ed25519 public key from the raw 32-byte RFC 8032 encoding. */
    private static PublicKey decodeRawPublicKey(byte[] raw) throws GeneralSecurityException {
        if (raw == null || raw.length != 32) {
            throw new InvalidKeyException("Ed25519 public key must be 32 bytes");
        }
        byte[] copy = raw.clone();
        boolean xOdd = (copy[31] & 0x80) != 0;
        copy[31] &= (byte) 0x7F;
        // little-endian -> big-endian for BigInteger
        for (int i = 0; i < copy.length / 2; i++) {
            byte tmp = copy[i];
            copy[i] = copy[copy.length - 1 - i];
            copy[copy.length - 1 - i] = tmp;
        }
        BigInteger y = new BigInteger(1, copy);
        EdECPoint point = new EdECPoint(xOdd, y);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePublic(new EdECPublicKeySpec(NamedParameterSpec.ED25519, point));
    }
}
