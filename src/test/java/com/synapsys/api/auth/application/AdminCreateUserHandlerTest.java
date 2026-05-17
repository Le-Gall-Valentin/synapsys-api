package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCreateUserHandlerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasherPort passwordHasher;

    private AdminCreateUserHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AdminCreateUserHandler(userRepository, passwordHasher);
    }

    @Test
    void superAdmin_can_create_admin() {
        User created = user("newadmin", Role.ADMIN);
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(created);

        User result = handler.create(cmd("newadmin", Role.ADMIN, Role.SUPER_ADMIN));

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(any());
    }

    @Test
    void superAdmin_can_create_user() {
        User created = user("newuser", Role.USER);
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(created);

        User result = handler.create(cmd("newuser", Role.USER, Role.SUPER_ADMIN));

        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void superAdmin_cannot_create_superAdmin() {
        assertThatThrownBy(() -> handler.create(cmd("sa", Role.SUPER_ADMIN, Role.SUPER_ADMIN)))
            .isInstanceOf(AuthException.InsufficientPermissions.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void admin_can_create_user() {
        User created = user("newuser", Role.USER);
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(created);

        User result = handler.create(cmd("newuser", Role.USER, Role.ADMIN));

        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void admin_cannot_create_admin() {
        assertThatThrownBy(() -> handler.create(cmd("newadmin", Role.ADMIN, Role.ADMIN)))
            .isInstanceOf(AuthException.InsufficientPermissions.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void user_cannot_create_anyone() {
        assertThatThrownBy(() -> handler.create(cmd("someone", Role.USER, Role.USER)))
            .isInstanceOf(AuthException.InsufficientPermissions.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void create_existingUsername_throwsUsernameAlreadyExists() {
        when(userRepository.findByUsername("existing"))
            .thenReturn(Optional.of(user("existing", Role.USER)));

        assertThatThrownBy(() -> handler.create(cmd("existing", Role.USER, Role.SUPER_ADMIN)))
            .isInstanceOf(AuthException.UsernameAlreadyExists.class);
        verify(userRepository, never()).save(any());
    }

    private AdminCreateUserCommand cmd(String username, Role targetRole, Role callerRole) {
        return new AdminCreateUserCommand(username, username + "@test.com", "pass", targetRole, callerRole);
    }

    private User user(String username, Role role) {
        return new User(UUID.randomUUID(), username, username + "@test.com", "hashed", role, true, Instant.now());
    }
}