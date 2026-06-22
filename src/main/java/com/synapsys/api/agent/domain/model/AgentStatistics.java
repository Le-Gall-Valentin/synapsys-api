package com.synapsys.api.agent.domain.model;

public record AgentStatistics(long active, long inactive, long pending, long revoked, long total) {}