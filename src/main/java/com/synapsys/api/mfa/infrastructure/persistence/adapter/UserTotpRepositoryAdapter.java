package com.synapsys.api.mfa.infrastructure.persistence.adapter;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpPort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import com.synapsys.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.synapsys.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.synapsys.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserTotpRepositoryAdapter implements UserTotpPort, UserTotpQueryPort {

    private final UserTotpJpaRepository jpa;
    private final TotpEncryptorFactory encryptorFactory;

    public UserTotpRepositoryAdapter(UserTotpJpaRepository jpa,
                                     TotpEncryptorFactory encryptorFactory) {
        this.jpa = jpa;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public Optional<UserTotpProfile> findById(UUID userId) {
        return jpa.findById(userId).map(e -> {
            Optional<String> secret = Optional.ofNullable(e.getTotpSecret())
                .map(enc -> encryptorFactory.forUser(userId).decrypt(enc));
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
        jpa.saveTotpSecretById(userId, encryptorFactory.forUser(userId).encrypt(secret));
    }

    @Override
    public boolean saveTotpSecretIfAbsent(UUID userId, String secret) {
        return jpa.saveTotpSecretIfAbsent(userId, encryptorFactory.forUser(userId).encrypt(secret)) > 0;
    }

    @Override
    public void enableTotp(UUID userId) { jpa.enableTotpById(userId); }

    @Override
    public void disableTotp(UUID userId) { jpa.disableTotpById(userId); }
}