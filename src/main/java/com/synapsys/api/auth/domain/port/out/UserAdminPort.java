package com.synapsys.api.auth.domain.port.out;

public interface UserAdminPort {
    /** Returns true if no users exist — used for initial seeding only. */
    boolean isEmpty();
}