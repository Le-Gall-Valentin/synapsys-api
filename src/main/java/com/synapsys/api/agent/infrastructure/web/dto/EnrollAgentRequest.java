package com.synapsys.api.agent.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EnrollAgentRequest(
    @Schema(description = "Token d'enrôlement en clair", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String token,
    @Schema(description = "Clé publique Ed25519 (32 octets) encodée en base64url", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String publicKey
) {}
