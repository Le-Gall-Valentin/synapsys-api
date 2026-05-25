package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.Role;
import java.util.UUID;

public interface DeactivateUserUseCase {
    void deactivate(UUID targetUserId, UUID callerId, Role callerRole);
}