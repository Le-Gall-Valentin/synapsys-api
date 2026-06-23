package com.synapsys.api.agent.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEnrollmentTokenRequest(
    @Schema(description = "Nom du serveur cible", example = "web-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 100) String serverName,

    @Schema(description = "Durée de validité en minutes ; omis = maximum configuré (24h par défaut). " +
        "Au-delà du maximum → 400.", example = "15")
    @Min(1) Integer ttlMinutes
) {}
