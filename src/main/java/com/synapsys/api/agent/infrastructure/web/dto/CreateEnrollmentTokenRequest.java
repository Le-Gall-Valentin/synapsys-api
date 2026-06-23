package com.synapsys.api.agent.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEnrollmentTokenRequest(
    @Schema(description = "Nom du serveur cible", example = "web-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 100) String serverName
) {}
