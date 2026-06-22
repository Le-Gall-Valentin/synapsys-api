package com.synapsys.api.agent.domain.model;

import java.util.UUID;

public record RevokeEnrollmentTokenCommand(UUID tokenId, UUID callerId) {}