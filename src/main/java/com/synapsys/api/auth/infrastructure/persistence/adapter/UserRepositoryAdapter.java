package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.CreateUserCommand;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.UserAdminPort;
import com.synapsys.api.auth.domain.port.out.UserCommandPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.auth.domain.port.out.UserTotpPort;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

// Implements UserRepository (queries), UserCommandPort (mutations), and UserAdminPort (admin checks),
// sharing a single JPA repository to avoid duplicating persistence logic across multiple adapters.
@Component
public class UserRepositoryAdapter implements UserRepository, UserCommandPort, UserAdminPort, UserTotpPort {

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
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toDomain);
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
            entity.setPasswordHash(command.password());
            entity.setRole(command.role());
            return toDomain(jpa.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw resolveConstraintViolation(e);
        }
    }

    private AuthException resolveConstraintViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String constraint = cve.getConstraintName();
            if (constraint != null) {
                if (constraint.contains("uq_users_email"))    return new AuthException.EmailAlreadyExists();
                if (constraint.contains("uq_users_username")) return new AuthException.UsernameAlreadyExists();
            }
        }
        return new AuthException.DataIntegrityError();
    }

    @Override
    public void saveTotpSecret(UUID userId, String secret) {
        jpa.saveTotpSecretById(userId, secret);
    }

    @Override
    public void enableTotp(UUID userId) {
        jpa.enableTotpById(userId);
    }

    @Override
    public void disableTotp(UUID userId) {
        jpa.disableTotpById(userId);
    }

    private User toDomain(UserEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(),
            e.getPasswordHash(), e.getRole(), e.isActive(), e.getCreatedAt(),
            e.getTotpSecret(), e.isTotpEnabled());
    }
}