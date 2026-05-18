package com.synapsys.api.auth.domain.model;

public record RegisterCommand(
    String username,
    String email,
    String password,
    Role role,
    Role callerRole
) {}