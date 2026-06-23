package com.synapsys.api.agent.application.port.in;

import java.util.UUID;

public interface HandleAgentDisconnectUseCase {
    void disconnect(UUID agentId, String ip, String nodeId);
}
