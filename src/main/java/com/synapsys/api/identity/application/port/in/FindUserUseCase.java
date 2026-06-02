package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface FindUserUseCase {
    Optional<User> findByUsername(String username);
    Optional<User> findById(UUID id);
}