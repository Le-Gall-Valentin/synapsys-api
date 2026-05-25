package com.synapsys.api.auth.domain.service;

import com.synapsys.api.auth.domain.model.Role;

public final class RoleCreationPolicy {

    private RoleCreationPolicy() {}

    public static boolean canCreate(Role callerRole, Role targetRole) {
        return RoleHierarchy.canManage(callerRole, targetRole);
    }
}