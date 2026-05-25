package com.synapsys.api.auth.domain.service;

import com.synapsys.api.auth.domain.model.Role;

public final class RoleDeactivationPolicy {

    private RoleDeactivationPolicy() {}

    public static boolean canDeactivate(Role callerRole, Role targetRole) {
        return RoleHierarchy.canManage(callerRole, targetRole);
    }
}