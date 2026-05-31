package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.port.out.CredentialSetupPort;
import com.synapsys.api.identity.domain.port.out.TotpRecordInitPort;
import com.synapsys.api.identity.domain.port.out.UserAdminPort;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedHandlerTest {

    @Mock UserAdminPort userAdminPort;
    @Mock UserCommandPort userCommandPort;
    @Mock CredentialSetupPort credentialSetupPort;
    @Mock TotpRecordInitPort totpRecordInitPort;

    private SeedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SeedHandler(userAdminPort, userCommandPort, credentialSetupPort, totpRecordInitPort);
    }

    @Test
    void seedInitialSuperAdmin_emptyDatabase_createsUser() {
        UUID userId = UUID.randomUUID();
        when(userAdminPort.isEmpty()).thenReturn(true);
        when(userCommandPort.createProfile(argThat(cmd ->
            cmd.username().equals("admin") &&
            cmd.email().equals("admin@test.com") &&
            cmd.role() == Role.SUPER_ADMIN
        ))).thenReturn(userId);

        handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret");

        verify(credentialSetupPort).setup(userId, "secret");
        verify(totpRecordInitPort).initForUser(userId);
    }

    @Test
    void seedInitialSuperAdmin_nonEmptyDatabase_skips() {
        when(userAdminPort.isEmpty()).thenReturn(false);

        handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret");

        verifyNoInteractions(userCommandPort);
        verifyNoInteractions(credentialSetupPort);
        verifyNoInteractions(totpRecordInitPort);
    }

    @Test
    void seedInitialSuperAdmin_concurrentDuplicate_completesNormally() {
        when(userAdminPort.isEmpty()).thenReturn(true);
        when(userCommandPort.createProfile(any())).thenThrow(new IdentityException.UsernameAlreadyExists());

        assertThatNoException().isThrownBy(() ->
            handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret")
        );
    }

    @Test
    void seedInitialSuperAdmin_emailAlreadyExists_completesNormally() {
        when(userAdminPort.isEmpty()).thenReturn(true);
        when(userCommandPort.createProfile(any())).thenThrow(new IdentityException.EmailAlreadyExists());

        assertThatNoException().isThrownBy(() ->
            handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret")
        );
    }
}