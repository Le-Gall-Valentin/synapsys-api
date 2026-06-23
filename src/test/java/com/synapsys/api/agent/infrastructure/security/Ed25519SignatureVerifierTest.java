package com.synapsys.api.agent.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

class Ed25519SignatureVerifierTest {

    private final Ed25519SignatureVerifier verifier = new Ed25519SignatureVerifier();
    private final byte[] message = "the-nonce-value".getBytes(StandardCharsets.UTF_8);

    private static KeyPair keyPair() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    private static byte[] sign(PrivateKey priv, byte[] msg) throws Exception {
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(priv);
        s.update(msg);
        return s.sign();
    }

    /** RFC 8032 raw encoding: y little-endian (32 bytes), top bit of last byte = x parity. */
    static byte[] encodeRaw(EdECPublicKey key) {
        byte[] y = key.getPoint().getY().toByteArray(); // big-endian, possibly with a leading 0x00
        byte[] le = new byte[32];
        for (int i = 0; i < y.length && i < 32; i++) {
            le[i] = y[y.length - 1 - i];
        }
        if (key.getPoint().isXOdd()) {
            le[31] |= (byte) 0x80;
        }
        return le;
    }

    @Test
    void verify_validSignature_returnsTrue() throws Exception {
        KeyPair kp = keyPair();
        byte[] raw = encodeRaw((EdECPublicKey) kp.getPublic());
        assertThat(verifier.verify(raw, message, sign(kp.getPrivate(), message))).isTrue();
    }

    @Test
    void verify_tamperedSignature_returnsFalse() throws Exception {
        KeyPair kp = keyPair();
        byte[] raw = encodeRaw((EdECPublicKey) kp.getPublic());
        byte[] sig = sign(kp.getPrivate(), message);
        sig[0] ^= 0x01;
        assertThat(verifier.verify(raw, message, sig)).isFalse();
    }

    @Test
    void verify_wrongKey_returnsFalse() throws Exception {
        KeyPair signer = keyPair();
        KeyPair other = keyPair();
        byte[] otherRaw = encodeRaw((EdECPublicKey) other.getPublic());
        assertThat(verifier.verify(otherRaw, message, sign(signer.getPrivate(), message))).isFalse();
    }

    @Test
    void verify_wrongKeyLength_returnsFalse() {
        assertThat(verifier.verify(new byte[31], message, new byte[64])).isFalse();
    }
}
