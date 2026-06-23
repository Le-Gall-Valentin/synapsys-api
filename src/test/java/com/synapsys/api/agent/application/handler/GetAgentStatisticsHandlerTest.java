package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.domain.model.AgentStatistics;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAgentStatisticsHandlerTest {

    @Mock AgentRepository agentRepository;
    @Mock AgentPresencePort presence;

    private Agent agent(UUID id, Instant firstConnectedAt) {
        return new Agent(id, "s", new byte[32], "fp", AgentLifecycleStatus.ENROLLED, UUID.randomUUID(),
            Instant.now(), UUID.randomUUID(), firstConnectedAt, null, null, null, null);
    }

    @Test
    void statistics_countsActiveInactivePendingRevoked() {
        UUID active = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        UUID pending = UUID.randomUUID();
        when(agentRepository.findAllNonRevoked()).thenReturn(List.of(
            agent(active, Instant.now()), agent(inactive, Instant.now()), agent(pending, null)));
        when(presence.presentAgentIds(any())).thenReturn(Set.of(active));
        when(agentRepository.countRevoked()).thenReturn(4L);

        AgentStatistics stats = new GetAgentStatisticsHandler(agentRepository, presence).statistics();

        assertThat(stats.active()).isEqualTo(1);
        assertThat(stats.inactive()).isEqualTo(1);
        assertThat(stats.pending()).isEqualTo(1);
        assertThat(stats.revoked()).isEqualTo(4);
        assertThat(stats.total()).isEqualTo(7);
    }
}
