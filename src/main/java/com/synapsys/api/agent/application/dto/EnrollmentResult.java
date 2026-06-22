package com.synapsys.api.agent.application.dto;

import java.util.UUID;

public record EnrollmentResult(UUID agentId, String serverName, String fingerprint) {}
