package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.application.dto.DeactivateUserCommand;

public interface DeactivateUserUseCase {
    void deactivate(DeactivateUserCommand command);
}