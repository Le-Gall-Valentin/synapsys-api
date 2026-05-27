package com.synapsys.api.auth.domain.port.out;

import com.synapsys.api.auth.domain.model.CreateUserCommand;
import com.synapsys.api.auth.domain.model.User;

import java.util.UUID;

public interface UserCommandPort {
    User save(CreateUserCommand command);
    void deactivate(UUID userId);
}