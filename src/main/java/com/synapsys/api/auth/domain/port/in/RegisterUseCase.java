package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.RegisterCommand;
import com.synapsys.api.auth.domain.model.User;

public interface RegisterUseCase {
    User register(RegisterCommand command);
}
