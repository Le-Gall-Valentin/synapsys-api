package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.domain.model.RevokeAgentCommand;

public interface RevokeAgentUseCase {
    void revoke(RevokeAgentCommand command);
}
