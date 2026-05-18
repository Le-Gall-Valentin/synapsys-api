package com.synapsys.api.auth.infrastructure.web.dto;

import com.synapsys.api.auth.domain.model.Role;
import java.util.UUID;

public record UserInfoResponse(UUID id, String username, Role role) {}