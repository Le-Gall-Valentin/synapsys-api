package com.synapsys.api.auth.infrastructure.web.dto;

import java.util.UUID;

public record UserInfoResponse(UUID id, String username, String role) {}