package com.synapsys.api.auth.domain.service;

import com.synapsys.api.auth.domain.model.Role;

/**
 * Encapsulates creation authorization: a caller may create a user only if their role
 * can manage the target role according to the hierarchy.
 */
public final class RoleCreationPolicy {

    private RoleCreationPolicy() {}

    public static boolean canCreate(Role callerRole, Role targetRole) {
        return RoleHierarchy.canManage(callerRole, targetRole);
    }
}