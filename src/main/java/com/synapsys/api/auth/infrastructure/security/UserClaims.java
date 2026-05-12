package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.Role;

import java.util.UUID;

public record UserClaims(UUID userId, Role role) {}