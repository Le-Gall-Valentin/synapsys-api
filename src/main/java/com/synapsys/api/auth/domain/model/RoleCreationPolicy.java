package com.synapsys.api.auth.domain.model;

public final class RoleCreationPolicy {

    private RoleCreationPolicy() {}

    public static boolean canCreate(Role callerRole, Role targetRole) {
        return switch (callerRole) {
            case SUPER_ADMIN -> targetRole == Role.ADMIN || targetRole == Role.USER;
            case ADMIN       -> targetRole == Role.USER;
            default          -> false;
        };
    }
}