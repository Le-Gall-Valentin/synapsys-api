package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterCommandTest {

    @Test
    void constructor_normalizesEmailToLowercase() {
        var cmd = new RegisterCommand("alice", "User@TEST.com", "pass", Role.USER);
        assertThat(cmd.email()).isEqualTo("user@test.com");
    }

    @Test
    void constructor_nullEmailIsToleratedWithoutNPE() {
        var cmd = new RegisterCommand("alice", null, "pass", Role.USER);
        assertThat(cmd.email()).isNull();
    }
}