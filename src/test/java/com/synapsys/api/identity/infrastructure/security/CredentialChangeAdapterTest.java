package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.authentication.application.port.in.ChangePasswordUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CredentialChangeAdapterTest {

    @Mock ChangePasswordUseCase changePasswordUseCase;

    @Test
    void changePassword_delegatesToChangePasswordUseCase() {
        CredentialChangeAdapter adapter = new CredentialChangeAdapter(changePasswordUseCase);
        UUID userId = UUID.randomUUID();

        adapter.changePassword(userId, "current", "newPass");

        verify(changePasswordUseCase).changePassword(userId, "current", "newPass");
    }
}