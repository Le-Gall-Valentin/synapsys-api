package com.synapsys.api.authentication.infrastructure.web.dto;

public record TotpRequiredResponse(boolean totpRequired) {
    public TotpRequiredResponse() { this(true); }
}