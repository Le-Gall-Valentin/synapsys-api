package com.synapsys.api.auth.application.dto;

import com.synapsys.api.shared.model.Role;
import java.util.UUID;

public record DeactivateUserCommand(
    UUID targetUserId,
    UUID callerId,
    Role callerRole
) {}
