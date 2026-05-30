package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.shared.model.Role;

import java.util.UUID;

public record UserClaims(UUID userId, Role role, String email) {}