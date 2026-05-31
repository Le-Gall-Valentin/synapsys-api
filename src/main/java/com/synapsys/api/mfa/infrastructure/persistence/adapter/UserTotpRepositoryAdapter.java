package com.synapsys.api.mfa.infrastructure.persistence.adapter;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpPort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import com.synapsys.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.synapsys.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserTotpRepositoryAdapter implements UserTotpPort, UserTotpQueryPort {

    private final UserTotpJpaRepository jpa;
    private final TextEncryptor encryptor;

    public UserTotpRepositoryAdapter(UserTotpJpaRepository jpa,
                                     @Qualifier("totpSecretEncryptor") TextEncryptor encryptor) {
        this.jpa = jpa;
        this.encryptor = encryptor;
    }

    @Override
    public Optional<UserTotpProfile> findById(UUID userId) {
        return jpa.findById(userId).map(e -> {
            String secret = e.getTotpSecret() != null ? encryptor.decrypt(e.getTotpSecret()) : null;
            return new UserTotpProfile(userId, e.isTotpEnabled(), secret);
        });
    }

    @Override
    public void createDefaultRecord(UUID userId) {
        UserTotpEntity e = new UserTotpEntity();
        e.setUserId(userId);
        jpa.save(e);
    }

    @Override
    public void saveTotpSecret(UUID userId, String secret) {
        jpa.saveTotpSecretById(userId, encryptor.encrypt(secret));
    }

    @Override
    public boolean saveTotpSecretIfAbsent(UUID userId, String secret) {
        return jpa.saveTotpSecretIfAbsent(userId, encryptor.encrypt(secret)) > 0;
    }

    @Override
    public void enableTotp(UUID userId) { jpa.enableTotpById(userId); }

    @Override
    public void disableTotp(UUID userId) { jpa.disableTotpById(userId); }
}