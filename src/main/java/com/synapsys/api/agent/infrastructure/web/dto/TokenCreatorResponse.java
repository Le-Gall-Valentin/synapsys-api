package com.synapsys.api.agent.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Créateur du token")
public record TokenCreatorResponse(
    UUID id,
    @Schema(description = "Username du créateur ; null si l'utilisateur a été supprimé") 
    String username
) {}