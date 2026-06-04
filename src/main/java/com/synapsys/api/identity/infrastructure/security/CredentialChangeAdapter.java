package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.authentication.application.port.in.ChangePasswordUseCase;
import com.synapsys.api.identity.domain.port.out.CredentialChangePort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CredentialChangeAdapter implements CredentialChangePort {

    private final ChangePasswordUseCase changePasswordUseCase;

    public CredentialChangeAdapter(ChangePasswordUseCase changePasswordUseCase) {
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @Override
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        changePasswordUseCase.changePassword(userId, currentPassword, newPassword);
    }
}