package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserAdminPort;
import com.synapsys.api.auth.domain.port.out.UserCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedHandlerTest {

    @Mock UserAdminPort userAdminPort;
    @Mock UserCommandPort userCommandPort;
    @Mock PasswordHasherPort passwordHasher;

    private SeedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SeedHandler(userAdminPort, userCommandPort, passwordHasher);
    }

    @Test
    void seedInitialSuperAdmin_emptyDatabase_createsUser() {
        when(userAdminPort.isEmpty()).thenReturn(true);
        when(passwordHasher.hash("secret")).thenReturn("hashed");

        handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret");

        verify(userCommandPort).save(argThat(cmd ->
            cmd.username().equals("admin") &&
            cmd.email().equals("admin@test.com") &&
            cmd.password().equals("hashed") &&
            cmd.role() == Role.SUPER_ADMIN
        ));
    }

    @Test
    void seedInitialSuperAdmin_nonEmptyDatabase_skips() {
        when(userAdminPort.isEmpty()).thenReturn(false);

        handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret");

        verifyNoInteractions(passwordHasher);
        verify(userCommandPort, never()).save(any());
    }

    @Test
    void seedInitialSuperAdmin_concurrentDuplicate_completesNormally() {
        when(userAdminPort.isEmpty()).thenReturn(true);
        when(passwordHasher.hash("secret")).thenReturn("hashed");
        doThrow(new AuthException.UsernameAlreadyExists()).when(userCommandPort).save(any());

        assertThatNoException().isThrownBy(() ->
            handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret")
        );
    }

    @Test
    void seedInitialSuperAdmin_emailAlreadyExists_completesNormally() {
        when(userAdminPort.isEmpty()).thenReturn(true);
        when(passwordHasher.hash("secret")).thenReturn("hashed");
        doThrow(new AuthException.EmailAlreadyExists()).when(userCommandPort).save(any());

        assertThatNoException().isThrownBy(() ->
            handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret")
        );
    }
}