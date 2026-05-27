package com.synapsys.api.auth.domain.model;

import java.util.Locale;
import java.util.Objects;

public record RegisterCommand(
    String username,
    String email,
    String password,
    Role role
) {
    public RegisterCommand {
        Objects.requireNonNull(email, "email must not be null");
        email = email.toLowerCase(Locale.ROOT);
    }
}