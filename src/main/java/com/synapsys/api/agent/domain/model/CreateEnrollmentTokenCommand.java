package com.synapsys.api.agent.domain.model;

import java.util.UUID;

public record CreateEnrollmentTokenCommand(String serverName, UUID createdBy) {}