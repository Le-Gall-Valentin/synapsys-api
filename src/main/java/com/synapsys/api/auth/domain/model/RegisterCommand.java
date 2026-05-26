package com.synapsys.api.auth.domain.model;

import java.util.Locale;

public record RegisterCommand(
    String username,
    String email,
    String password,
    Role role
) {
    public RegisterCommand {
        email = email.toLowerCase(Locale.ROOT);
    }
}