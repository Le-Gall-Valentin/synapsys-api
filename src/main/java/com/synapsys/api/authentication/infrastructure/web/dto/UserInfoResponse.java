package com.synapsys.api.authentication.infrastructure.web.dto;

import com.synapsys.api.shared.model.Role;
import java.time.Instant;
import java.util.UUID;

public record UserInfoResponse(UUID id, String username, String email, Role role, Instant createdAt, boolean totpEnabled) {}