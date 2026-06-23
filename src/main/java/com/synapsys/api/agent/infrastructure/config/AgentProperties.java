package com.synapsys.api.agent.infrastructure.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "synapsys.agent")
public record AgentProperties(
    @Positive @DefaultValue("24") int enrollmentTokenValidityHours,
    @Positive @DefaultValue("30") int challengeTtlSeconds,
    @Positive @DefaultValue("90") int presenceTtlSeconds,
    @Positive @DefaultValue("10") int maxConnectionsPerIp,
    @DefaultValue("synenr_") String tokenPrefix,
    @DefaultValue("/ws/agents") String websocketPath
) {}
