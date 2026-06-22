package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.application.dto.EnrollmentResult;
import com.synapsys.api.agent.domain.model.EnrollAgentCommand;

public interface EnrollAgentUseCase {
    EnrollmentResult enroll(EnrollAgentCommand command);
}
