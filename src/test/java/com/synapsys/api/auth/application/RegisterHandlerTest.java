package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterHandlerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasherPort passwordHasher;

    private RegisterHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterHandler(userRepository, passwordHasher);
    }

    @Test
    void register_hashesPasswordAndSavesUser() {
        User created = new User(UUID.randomUUID(), "newuser", "new@test.com",
            "hashed", Role.USER, true, Instant.now());
        when(passwordHasher.hash("plaintext")).thenReturn("hashed");
        when(userRepository.save(any(CreateUserCommand.class))).thenReturn(created);

        User result = handler.register(new RegisterCommand("newuser", "new@test.com", "plaintext", Role.USER));

        assertThat(result.username()).isEqualTo("newuser");
        verify(passwordHasher).hash("plaintext");
        verify(userRepository).save(argThat(cmd ->
            cmd.username().equals("newuser") && cmd.passwordHash().equals("hashed")
        ));
    }

    @Test
    void register_existingUsername_throwsUsernameAlreadyExists() {
        User existing = new User(UUID.randomUUID(), "newuser", "x@test.com", "h", Role.USER, true, Instant.now());
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> handler.register(
            new RegisterCommand("newuser", "new@test.com", "pass", Role.USER)
        )).isInstanceOf(AuthException.UsernameAlreadyExists.class);
        verify(userRepository, never()).save(any(CreateUserCommand.class));
    }
}