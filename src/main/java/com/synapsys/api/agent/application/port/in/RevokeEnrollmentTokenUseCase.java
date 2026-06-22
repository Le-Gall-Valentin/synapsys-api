package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.domain.model.RevokeEnrollmentTokenCommand;

public interface RevokeEnrollmentTokenUseCase {
    void revoke(RevokeEnrollmentTokenCommand command);
}
