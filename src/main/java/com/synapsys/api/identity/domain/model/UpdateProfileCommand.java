package com.synapsys.api.identity.domain.model;

import java.util.UUID;

public record UpdateProfileCommand(UUID userId, String username, String email) {}