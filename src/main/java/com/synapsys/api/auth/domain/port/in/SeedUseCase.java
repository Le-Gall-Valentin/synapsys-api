package com.synapsys.api.auth.domain.port.in;

public interface SeedUseCase {
    /**
     * Seeds the initial SUPER_ADMIN if the database is empty.
     * No-op if users already exist. Idempotent.
     */
    void seedInitialSuperAdmin(String username, String email, String password);
}