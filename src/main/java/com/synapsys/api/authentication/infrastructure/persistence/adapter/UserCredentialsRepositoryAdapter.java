package com.synapsys.api.authentication.infrastructure.persistence.adapter;

import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.domain.port.out.UserCredentialsPort;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.identity.application.service.UserCredentialsService;
import com.synapsys.api.mfa.application.service.TotpStatusService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserCredentialsRepositoryAdapter implements UserCredentialsPort {

    private final UserCredentialsService identityUserService;
    private final UserCredentialJpaRepository credentialRepo;
    private final TotpStatusService totpStatusService;

    public UserCredentialsRepositoryAdapter(UserCredentialsService identityUserService,
                                            UserCredentialJpaRepository credentialRepo,
                                            TotpStatusService totpStatusService) {
        this.identityUserService = identityUserService;
        this.credentialRepo = credentialRepo;
        this.totpStatusService = totpStatusService;
    }

    @Override
    public Optional<UserCredentials> findByUsername(String username) {
        return identityUserService.findByUsername(username)
            .flatMap(userInfo -> credentialRepo.findById(userInfo.id())
                .map(cred -> new UserCredentials(
                    userInfo.id(),
                    userInfo.username(),
                    userInfo.email(),
                    cred.getPasswordHash(),
                    userInfo.isActive(),
                    userInfo.role(),
                    totpStatusService.isTotpEnabled(userInfo.id())
                )));
    }

    @Override
    public Optional<UserCredentials> findById(UUID id) {
        return identityUserService.findById(id)
            .flatMap(userInfo -> credentialRepo.findById(userInfo.id())
                .map(cred -> new UserCredentials(
                    userInfo.id(),
                    userInfo.username(),
                    userInfo.email(),
                    cred.getPasswordHash(),
                    userInfo.isActive(),
                    userInfo.role(),
                    totpStatusService.isTotpEnabled(userInfo.id())
                )));
    }
}