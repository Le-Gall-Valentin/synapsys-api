package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.auth.application.service.CredentialSetupService;
import com.synapsys.api.identity.domain.port.out.CredentialSetupPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CredentialSetupAdapter implements CredentialSetupPort {

    private final CredentialSetupService credentialSetupService;

    public CredentialSetupAdapter(CredentialSetupService credentialSetupService) {
        this.credentialSetupService = credentialSetupService;
    }

    @Override
    public void setup(UUID userId, String rawPassword) {
        credentialSetupService.setup(userId, rawPassword);
    }
}