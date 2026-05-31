package com.synapsys.api.authentication.domain.model;

import com.synapsys.api.shared.model.Role;
import java.util.UUID;

public record UserCredentials(
    UUID id,
    String username,
    String email,
    String passwordHash,
    boolean isActive,
    Role role,
    boolean totpEnabled
) {}