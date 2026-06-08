package com.synapsys.api.identity.infrastructure.web.dto;

import com.synapsys.api.shared.model.Role;
import java.time.Instant;
import java.util.UUID;

public record UserAdminItemResponse(
    UUID id,
    String username,
    String email,
    Role role,
    boolean isActive,
    Instant createdAt,
    boolean totpEnabled
) {}