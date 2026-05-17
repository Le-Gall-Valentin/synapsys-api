package com.synapsys.api.auth.domain.port.out;

import com.synapsys.api.auth.domain.model.CreateUserCommand;
import com.synapsys.api.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(UUID id);
    User save(CreateUserCommand command);
}
