package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.domain.model.VerifyHandshakeCommand;

public interface VerifyAgentHandshakeUseCase {
    void verify(VerifyHandshakeCommand command, String ip, String nodeId);
}
