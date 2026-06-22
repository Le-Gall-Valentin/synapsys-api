package com.synapsys.api.agent.domain.model;

import java.util.UUID;

public record VerifyHandshakeCommand(UUID agentId, String connectionId, byte[] signature) {}
