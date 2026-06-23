package com.synapsys.api.agent.domain.model;

import java.time.Duration;
import java.util.UUID;

public record CreateEnrollmentTokenCommand(String serverName, Duration ttl, UUID createdBy) {}