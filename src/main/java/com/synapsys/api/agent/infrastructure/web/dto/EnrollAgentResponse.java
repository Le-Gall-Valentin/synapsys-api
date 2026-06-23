package com.synapsys.api.agent.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Résultat de l'enrôlement - éléments de connexion de l'agent")
public record EnrollAgentResponse(UUID agentId, String serverName, String fingerprint, String websocketPath) {}
