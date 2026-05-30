package com.synapsys.api.auth.domain.model;

import com.synapsys.api.shared.model.Role;

public record CreateUserCommand(
    String username,
    String email,
    String password,
    Role role
) {}
