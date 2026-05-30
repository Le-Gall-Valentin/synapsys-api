package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;

public record RegisterCommand(String username, String email, String rawPassword, Role role) {}