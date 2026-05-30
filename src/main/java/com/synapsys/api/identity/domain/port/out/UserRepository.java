package com.synapsys.api.identity.domain.port.out;

import com.synapsys.api.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
}