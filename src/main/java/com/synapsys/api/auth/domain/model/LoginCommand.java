package com.synapsys.api.auth.domain.model;

public record LoginCommand(String username, String password) {
    public LoginCommand {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("password required");
    }
}