package com.synapsys.api.auth.domain.model;

import java.util.UUID;

public record DeactivateUserCommand(
    UUID targetUserId,
    UUID callerId,
    Role callerRole
) {}