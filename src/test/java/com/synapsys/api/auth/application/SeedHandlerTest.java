package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
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

    @Mock UserRepository userRepository;
    @Mock PasswordHasherPort passwordHasher;

    private SeedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SeedHandler(userRepository, passwordHasher);
    }

    @Test
    void seedInitialSuperAdmin_emptyDatabase_createsUser() {
        when(userRepository.isEmpty()).thenReturn(true);
        when(passwordHasher.hash("secret")).thenReturn("hashed");

        handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret");

        verify(userRepository).save(argThat(cmd ->
            cmd.username().equals("admin") &&
            cmd.email().equals("admin@test.com") &&
            cmd.passwordHash().equals("hashed") &&
            cmd.role() == Role.SUPER_ADMIN
        ));
    }

    @Test
    void seedInitialSuperAdmin_nonEmptyDatabase_skips() {
        when(userRepository.isEmpty()).thenReturn(false);

        handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret");

        verifyNoInteractions(passwordHasher);
        verify(userRepository, never()).save(any());
    }

    @Test
    void seedInitialSuperAdmin_concurrentDuplicate_completesNormally() {
        when(userRepository.isEmpty()).thenReturn(true);
        when(passwordHasher.hash("secret")).thenReturn("hashed");
        doThrow(new AuthException.UsernameAlreadyExists()).when(userRepository).save(any());

        assertThatNoException().isThrownBy(() ->
            handler.seedInitialSuperAdmin("admin", "admin@test.com", "secret")
        );
    }
}