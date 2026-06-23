package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.domain.model.VerifyHandshakeCommand;

import java.util.UUID;

public interface VerifyAgentHandshakeUseCase {
    void verify(VerifyHandshakeCommand command, String ip, String nodeId);

    /**
     * Re-checks that the agent is still connectable (not revoked) after its live session has been
     * registered. Closes the race where a revoke commits during the post-commit window between the
     * handshake transaction and session registration, causing the revoke fan-out close to miss the
     * not-yet-registered session.
     */
    boolean isConnectable(UUID agentId);
}
