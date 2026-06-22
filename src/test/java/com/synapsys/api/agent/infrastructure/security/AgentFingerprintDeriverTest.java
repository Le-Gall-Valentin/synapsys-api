package com.synapsys.api.agent.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentFingerprintDeriverTest {

    private final AgentFingerprintDeriver deriver = new AgentFingerprintDeriver();

    @Test
    void fingerprint_isDeterministic64CharHex() {
        byte[] key = new byte[32];
        String fp = deriver.fingerprint(key);
        assertThat(fp).hasSize(64).matches("[0-9a-f]+");
        assertThat(deriver.fingerprint(key)).isEqualTo(fp);
    }

    @Test
    void fingerprint_differsForDifferentKeys() {
        byte[] k1 = new byte[32];
        byte[] k2 = new byte[32];
        k2[0] = 1;
        assertThat(deriver.fingerprint(k1)).isNotEqualTo(deriver.fingerprint(k2));
    }
}
