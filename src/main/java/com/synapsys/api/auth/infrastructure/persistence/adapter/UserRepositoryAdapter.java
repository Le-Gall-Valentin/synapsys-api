package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.CreateUserCommand;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
    public boolean isEmpty() {
        return jpa.count() == 0;
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
    public void deactivate(UUID userId) {
        jpa.deactivateById(userId);
    }

    @Override
    public User save(CreateUserCommand command) {
        try {
            UserEntity entity = new UserEntity();
            entity.setUsername(command.username());
            entity.setEmail(command.email());
            entity.setPasswordHash(command.passwordHash());
            entity.setRole(command.role());
            return toDomain(jpa.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw resolveConstraintViolation(e);
        }
    }

    private AuthException resolveConstraintViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String name = cve.getConstraintName();
            if (name != null) {
                return switch (name) {
                    case "uq_users_email"    -> new AuthException.EmailAlreadyExists();
                    case "uq_users_username" -> new AuthException.UsernameAlreadyExists();
                    default -> new AuthException.DataIntegrityError(name);
                };
            }
        }
        return new AuthException.DataIntegrityError(null);
    }

    private User toDomain(UserEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(),
            e.getPasswordHash(), e.getRole(), e.isActive(), e.getCreatedAt());
    }
}