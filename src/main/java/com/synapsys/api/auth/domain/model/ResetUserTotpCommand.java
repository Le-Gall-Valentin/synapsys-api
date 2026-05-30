package com.synapsys.api.auth.domain.model;

import java.util.UUID;

public record ResetUserTotpCommand(UUID targetUserId, UUID callerId, Role callerRole) {}