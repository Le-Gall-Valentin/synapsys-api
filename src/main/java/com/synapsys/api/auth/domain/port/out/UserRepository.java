package com.synapsys.api.auth.domain.port.out;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(UUID id);
    boolean existsAny();
    User save(String username, String email, String passwordHash, Role role);
}