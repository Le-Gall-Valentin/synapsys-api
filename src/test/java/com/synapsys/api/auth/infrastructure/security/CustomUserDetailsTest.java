package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void getPassword_returnsEmptyString() {
        CustomUserDetails details = new CustomUserDetails(UUID.randomUUID(), Role.USER);
        assertThat(details.getPassword()).isEqualTo("");
    }
}