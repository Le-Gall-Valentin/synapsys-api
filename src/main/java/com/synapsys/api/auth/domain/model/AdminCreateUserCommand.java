package com.synapsys.api.auth.domain.model;

public record AdminCreateUserCommand(
    String username,
    String email,
    String password,
    Role targetRole,
    Role callerRole
) {}