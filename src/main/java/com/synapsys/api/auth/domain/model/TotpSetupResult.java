package com.synapsys.api.auth.domain.model;

public record TotpSetupResult(String secret, String otpauthUri) {}