package com.synapsys.api.auth.domain.model;

import com.synapsys.api.shared.model.Role;
import java.util.Locale;
import java.util.Objects;

public record RegisterCommand(
    String username,
    String email,
    String password,
    Role role
) {
    public RegisterCommand {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(role, "role must not be null");
        email = email.toLowerCase(Locale.ROOT);
    }
}