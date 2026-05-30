package com.synapsys.api.auth.domain.model;

import com.synapsys.api.shared.model.Role;
import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String username,
    String email,
    String passwordHash,
    Role role,
    boolean isActive,
    Instant createdAt,
    String totpSecret,
    boolean totpEnabled
) {}