package com.synapsys.api.auth.domain.service;

import com.synapsys.api.auth.domain.model.Role;

public final class RoleDeactivationPolicy {

    private RoleDeactivationPolicy() {}

    public static boolean canDeactivate(Role callerRole, Role targetRole) {
        return switch (callerRole) {
            case SUPER_ADMIN -> targetRole == Role.ADMIN || targetRole == Role.USER;
            case ADMIN       -> targetRole == Role.USER;
            default          -> false;
        };
    }
}