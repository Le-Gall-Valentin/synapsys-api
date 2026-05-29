package com.synapsys.api.auth.domain.model;

public sealed interface LoginResult permits LoginResult.Success, LoginResult.TotpRequired {

    record Success(AuthTokens tokens, User user) implements LoginResult {}

    record TotpRequired(String challengeId) implements LoginResult {}
}