package com.synapsys.api.agent.domain.model;

import java.util.UUID;

public record NewAgent(String serverName, byte[] publicKey, String fingerprint, UUID enrollmentTokenId, UUID enrolledBy) {}
