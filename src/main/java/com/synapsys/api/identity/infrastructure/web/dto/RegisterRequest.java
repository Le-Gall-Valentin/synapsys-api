package com.synapsys.api.identity.infrastructure.web.dto;

import com.synapsys.api.shared.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 72)
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
        message = "must contain at least one uppercase letter, one digit, and one special character"
    )
    String password,
    @NotNull Role role
) {
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + ", role=" + role + "]";
    }
}