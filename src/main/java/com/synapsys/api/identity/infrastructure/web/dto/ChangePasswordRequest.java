package com.synapsys.api.identity.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8, max = 72)
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
        message = "must contain at least one uppercase letter, one digit, and one special character"
    )
    String newPassword
) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[]";
    }
}