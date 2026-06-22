package com.synapsys.api.agent.domain.port.out;

import java.util.UUID;

public interface AgentConnectionRegistryPort {
    /** Requests immediate close of the agent's live connection on whichever node holds it. */
    void requestClose(UUID agentId);
}
