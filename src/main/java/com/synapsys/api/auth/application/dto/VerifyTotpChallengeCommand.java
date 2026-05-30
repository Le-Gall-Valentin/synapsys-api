package com.synapsys.api.auth.application.dto;

public record VerifyTotpChallengeCommand(String challengeId, String code) {}
