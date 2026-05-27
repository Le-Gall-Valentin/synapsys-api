package com.synapsys.api.auth.infrastructure.web.dto;

import com.synapsys.api.auth.domain.model.Role;
import java.util.UUID;

// Role is imported from the domain model intentionally: the web layer exposes domain values directly.
public record UserInfoResponse(UUID id, String username, Role role) {}