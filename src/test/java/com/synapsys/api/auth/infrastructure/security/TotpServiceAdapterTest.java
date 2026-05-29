package com.synapsys.api.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceAdapterTest {

    private TotpServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TotpServiceAdapter();
    }

    @Test
    void generateSecret_returns16CharBase32String() {
        String secret = adapter.generateSecret();

        assertThat(secret).isNotBlank();
        assertThat(secret).hasSize(32);
        assertThat(secret).matches("[A-Z2-7]+");
    }

    @Test
    void generateSecret_producesUniqueSecrets() {
        String s1 = adapter.generateSecret();
        String s2 = adapter.generateSecret();

        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    void buildOtpauthUri_returnsCorrectFormat() {
        String secret = "JBSWY3DPEHPK3PXP";
        String uri = adapter.buildOtpauthUri(secret, "user@synapsys.io");

        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("secret=" + secret);
        assertThat(uri).contains("issuer=SynapSys");
        assertThat(uri).contains("algorithm=SHA256");
        assertThat(uri).contains("user%40synapsys.io");
    }

    @Test
    void isValid_invalidCode_returnsFalse() {
        String secret = adapter.generateSecret();

        assertThat(adapter.isValid(secret, "000000")).isFalse();
    }
}