package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final TextEncryptor encryptor;

    public UserRepositoryAdapter(UserJpaRepository jpa,
                                 @Qualifier("totpSecretEncryptor") TextEncryptor encryptor) {
        this.jpa = jpa;
        this.encryptor = encryptor;
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

    public void saveTotpSecret(UUID userId, String secret) {
        jpa.saveTotpSecretById(userId, encryptor.encrypt(secret));
    }

    public boolean saveTotpSecretIfAbsent(UUID userId, String secret) {
        return jpa.saveTotpSecretIfAbsent(userId, encryptor.encrypt(secret)) > 0;
    }

    public void enableTotp(UUID userId) {
        jpa.enableTotpById(userId);
    }

    public void disableTotp(UUID userId) {
        jpa.disableTotpById(userId);
    }

    private User toDomain(UserEntity e) {
        String totpSecret = e.getTotpSecret() != null ? encryptor.decrypt(e.getTotpSecret()) : null;
        return new User(e.getId(), e.getUsername(), e.getEmail(),
            e.getPasswordHash(), e.getRole(), e.isActive(), e.getCreatedAt(),
            totpSecret, e.isTotpEnabled());
    }
}