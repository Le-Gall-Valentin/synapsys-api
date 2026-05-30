package com.synapsys.api.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpCodeRequest(
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "must be exactly 6 digits")
    String code
) {}