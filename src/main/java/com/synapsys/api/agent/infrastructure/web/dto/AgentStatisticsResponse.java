package com.synapsys.api.agent.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statistiques de la flotte d'agents")
public record AgentStatisticsResponse(long active, long inactive, long pending, long revoked, long total) {}
