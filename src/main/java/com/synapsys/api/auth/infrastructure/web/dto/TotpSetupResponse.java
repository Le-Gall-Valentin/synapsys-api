package com.synapsys.api.auth.infrastructure.web.dto;

public record TotpSetupResponse(String otpauthUri, String secret) {}