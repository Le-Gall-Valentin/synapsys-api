package com.synapsys.api.auth.infrastructure.web.dto;

import com.synapsys.api.shared.model.Role;
import java.util.UUID;

public record UserInfoResponse(UUID id, String username, Role role, boolean totpEnabled) {}