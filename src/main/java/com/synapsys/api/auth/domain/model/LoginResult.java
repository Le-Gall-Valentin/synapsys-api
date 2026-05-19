package com.synapsys.api.auth.domain.model;

public record LoginResult(AuthTokens tokens, User user) {}