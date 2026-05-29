package com.synapsys.api.auth.infrastructure.web.dto;

public record TotpRequiredResponse(boolean totpRequired) {
    public TotpRequiredResponse() { this(true); }
}