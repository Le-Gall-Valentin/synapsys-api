package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsAny() {
        return jpa.count() > 0;
    }

    @Override
    public User save(String username, String email, String passwordHash, Role role) {
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setEmail(email);
        entity.setPasswordHash(passwordHash);
        entity.setRole(role);
        return toDomain(jpa.save(entity));
    }

    private User toDomain(UserEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(),
            e.getPasswordHash(), e.getRole(), e.isActive(), e.getCreatedAt());
    }
}