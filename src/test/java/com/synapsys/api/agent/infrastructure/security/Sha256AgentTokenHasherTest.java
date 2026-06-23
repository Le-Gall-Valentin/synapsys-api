package com.synapsys.api.agent.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256AgentTokenHasherTest {

    private final Sha256AgentTokenHasher hasher = new Sha256AgentTokenHasher();

    @Test
    void producesDeterministic64CharHex() {
        String h = hasher.hash("synenr_abc");
        assertThat(h).hasSize(64).matches("[0-9a-f]+");
        assertThat(hasher.hash("synenr_abc")).isEqualTo(h);
    }

    @Test
    void differentInputsDiffer() {
        assertThat(hasher.hash("a")).isNotEqualTo(hasher.hash("b"));
    }
}
