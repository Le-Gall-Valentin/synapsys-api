package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
import com.synapsys.api.shared.service.RoleHierarchy;
import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String username,
    String email,
    Role role,
    boolean isActive,
    Instant createdAt
) {
    public void ensureActive() {
        if (!isActive) throw new IdentityException.UserAlreadyInactive();
    }

    public void ensureInactive() {
        if (isActive) throw new IdentityException.UserAlreadyActive();
    }

    public void ensureCanBeDeactivatedBy(Role callerRole) {
        if (!RoleHierarchy.canManage(callerRole, this.role)) {
            throw new IdentityException.InsufficientPermissions();
        }
    }

    public void ensureCanBeActivatedBy(Role callerRole) {
        if (!RoleHierarchy.canManage(callerRole, this.role)) {
            throw new IdentityException.InsufficientPermissions();
        }
    }
}