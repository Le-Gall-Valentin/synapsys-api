package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
import java.util.Locale;

public record RegisterCommand(String username, String email, String rawPassword, Role role) {
    public RegisterCommand {
        email = email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "RegisterCommand[username=" + username + ", email=" + email + ", role=" + role + "]";
    }
}