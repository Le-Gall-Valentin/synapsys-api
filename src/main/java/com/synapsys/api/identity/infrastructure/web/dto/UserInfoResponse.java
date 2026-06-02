package com.synapsys.api.identity.infrastructure.web.dto;

import com.synapsys.api.shared.model.Role;

import java.util.UUID;

public record UserInfoResponse(UUID id, String username, Role role) {}