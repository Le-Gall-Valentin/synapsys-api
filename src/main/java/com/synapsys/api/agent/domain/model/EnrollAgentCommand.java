package com.synapsys.api.agent.domain.model;

public record EnrollAgentCommand(String rawToken, byte[] publicKey) {}
