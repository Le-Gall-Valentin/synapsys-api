package com.synapsys.api.auth.domain.service;

import com.synapsys.api.auth.domain.model.Role;

/**
 * Encapsulates deactivation authorization: a caller may deactivate a user only if their role
 * can manage the target role according to the hierarchy.
 */
public final class RoleDeactivationPolicy {

    private RoleDeactivationPolicy() {}

    public static boolean canDeactivate(Role callerRole, Role targetRole) {
        return RoleHierarchy.canManage(callerRole, targetRole);
    }
}