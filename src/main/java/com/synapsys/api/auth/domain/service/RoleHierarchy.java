package com.synapsys.api.auth.domain.service;

import com.synapsys.api.auth.domain.model.Role;

final class RoleHierarchy {
    private RoleHierarchy() {}

    static boolean canManage(Role callerRole, Role targetRole) {
        return switch (callerRole) {
            case SUPER_ADMIN -> targetRole == Role.ADMIN || targetRole == Role.USER;
            case ADMIN       -> targetRole == Role.USER;
            default          -> false;
        };
    }
}