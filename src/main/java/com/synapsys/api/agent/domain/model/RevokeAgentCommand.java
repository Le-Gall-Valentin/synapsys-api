package com.synapsys.api.agent.domain.model;

import java.util.UUID;

public record RevokeAgentCommand(UUID agentId, UUID callerId) {}
