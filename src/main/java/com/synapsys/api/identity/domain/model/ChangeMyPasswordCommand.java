package com.synapsys.api.identity.domain.model;

import java.util.UUID;

public record ChangeMyPasswordCommand(UUID userId, String currentPassword, String newPassword) {}