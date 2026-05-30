package com.synapsys.api.mfa.infrastructure.web.dto;

public record TotpSetupResponse(String otpauthUri, String secret) {}