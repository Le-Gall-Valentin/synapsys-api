package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.domain.model.CreateEnrollmentTokenCommand;
import com.synapsys.api.agent.domain.model.IssuedToken;

public interface CreateEnrollmentTokenUseCase {
    IssuedToken create(CreateEnrollmentTokenCommand command);
}
