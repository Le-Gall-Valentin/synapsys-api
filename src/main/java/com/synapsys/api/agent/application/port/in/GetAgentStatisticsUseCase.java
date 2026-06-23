package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.domain.model.AgentStatistics;

public interface GetAgentStatisticsUseCase {
    AgentStatistics statistics();
}
