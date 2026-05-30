package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.application.dto.RegisterCommand;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.shared.model.Role;

public interface RegisterUseCase {
    User register(RegisterCommand command, Role callerRole);
}