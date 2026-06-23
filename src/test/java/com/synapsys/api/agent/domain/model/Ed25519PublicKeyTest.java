package com.synapsys.api.agent.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Ed25519PublicKeyTest {

    @Test
    void accepts32Bytes() {
        var key = new Ed25519PublicKey(new byte[32]);
        assertThat(key.value()).hasSize(32);
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> new Ed25519PublicKey(new byte[31]))
            .isInstanceOf(AgentException.InvalidPublicKey.class);
        assertThatThrownBy(() -> new Ed25519PublicKey(new byte[33]))
            .isInstanceOf(AgentException.InvalidPublicKey.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Ed25519PublicKey(null))
            .isInstanceOf(AgentException.InvalidPublicKey.class);
    }

    @Test
    void defensivelyCopiesInputAndOutput() {
        byte[] raw = new byte[32];
        var key = new Ed25519PublicKey(raw);
        raw[0] = 1;                       // mutate caller's array
        assertThat(key.value()[0]).isZero();
        key.value()[0] = 2;               // mutate returned array
        assertThat(key.value()[0]).isZero();
    }
}
