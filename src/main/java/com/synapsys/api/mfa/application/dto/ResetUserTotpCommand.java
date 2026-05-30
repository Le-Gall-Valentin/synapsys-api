package com.synapsys.api.mfa.application.dto;

import com.synapsys.api.shared.model.Role;
import java.util.UUID;

public record ResetUserTotpCommand(UUID targetUserId, UUID callerId, Role callerRole) {}
