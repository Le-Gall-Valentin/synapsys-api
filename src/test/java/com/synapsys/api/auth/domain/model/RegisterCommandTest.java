package com.synapsys.api.auth.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RegisterCommandTest {

    @Test
    void constructor_throwsOnNullUsername() {
        assertThatNullPointerException().isThrownBy(() ->
            new RegisterCommand(null, "user@test.com", "password", Role.USER));
    }

    @Test
    void constructor_throwsOnNullPassword() {
        assertThatNullPointerException().isThrownBy(() ->
            new RegisterCommand("alice", "user@test.com", null, Role.USER));
    }

    @Test
    void constructor_throwsOnNullRole() {
        assertThatNullPointerException().isThrownBy(() ->
            new RegisterCommand("alice", "user@test.com", "password", null));
    }

    @Test
    void constructor_normalizesEmailToLowercase() {
        var cmd = new RegisterCommand("alice", "User@TEST.com", "password", Role.USER);
        assertThat(cmd.email()).isEqualTo("user@test.com");
    }
}