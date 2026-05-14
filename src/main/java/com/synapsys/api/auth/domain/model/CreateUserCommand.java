package com.synapsys.api.auth.domain.model;

public record CreateUserCommand(
    String username,
    String email,
    String passwordHash,
    Role role
) {}