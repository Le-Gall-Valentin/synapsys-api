package com.synapsys.api.agent.application.port.in;

import java.util.UUID;

public interface RecordAgentHeartbeatUseCase {
    void heartbeat(UUID agentId, String nodeId, String ip);
}
