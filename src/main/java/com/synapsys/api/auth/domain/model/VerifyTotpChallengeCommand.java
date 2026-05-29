package com.synapsys.api.auth.domain.model;

public record VerifyTotpChallengeCommand(String challengeId, String code) {}