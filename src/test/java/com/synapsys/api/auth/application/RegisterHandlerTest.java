package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterHandlerTest {

    @Mock UserCommandPort userCommandPort;
    @Mock PasswordHasherPort passwordHasher;

    private RegisterHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterHandler(userCommandPort, passwordHasher);
    }

    @Test
    void superAdmin_can_create_admin() {
        User created = user("newadmin", Role.ADMIN);
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        when(userCommandPort.save(any())).thenReturn(created);

        User result = handler.register(cmd("newadmin", Role.ADMIN), Role.SUPER_ADMIN);

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verify(userCommandPort).save(any());
    }

    @Test
    void superAdmin_can_create_user() {
        User created = user("newuser", Role.USER);
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        when(userCommandPort.save(any())).thenReturn(created);

        User result = handler.register(cmd("newuser", Role.USER), Role.SUPER_ADMIN);

        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void superAdmin_cannot_create_superAdmin() {
        assertThatThrownBy(() -> handler.register(cmd("sa", Role.SUPER_ADMIN), Role.SUPER_ADMIN))
            .isInstanceOf(AuthException.InsufficientPermissions.class);
        verifyNoInteractions(userCommandPort);
    }

    @Test
    void admin_can_create_user() {
        User created = user("newuser", Role.USER);
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        when(userCommandPort.save(any())).thenReturn(created);

        User result = handler.register(cmd("newuser", Role.USER), Role.ADMIN);

        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void admin_cannot_create_admin() {
        assertThatThrownBy(() -> handler.register(cmd("newadmin", Role.ADMIN), Role.ADMIN))
            .isInstanceOf(AuthException.InsufficientPermissions.class);
        verifyNoInteractions(userCommandPort);
    }

    @Test
    void user_cannot_create_anyone() {
        assertThatThrownBy(() -> handler.register(cmd("someone", Role.USER), Role.USER))
            .isInstanceOf(AuthException.InsufficientPermissions.class);
        verifyNoInteractions(userCommandPort);
    }

    @Test
    void register_existingUsername_throwsUsernameAlreadyExists() {
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        when(userCommandPort.save(any())).thenThrow(new AuthException.UsernameAlreadyExists());

        assertThatThrownBy(() -> handler.register(cmd("existing", Role.USER), Role.SUPER_ADMIN))
            .isInstanceOf(AuthException.UsernameAlreadyExists.class);
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExists() {
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        when(userCommandPort.save(any())).thenThrow(new AuthException.EmailAlreadyExists());

        assertThatThrownBy(() -> handler.register(
            new RegisterCommand("newuser", "dup@test.com", "pass", Role.USER), Role.SUPER_ADMIN
        )).isInstanceOf(AuthException.EmailAlreadyExists.class);
    }

    @Test
    void register_emailIsNormalized() {
        when(passwordHasher.hash("password123")).thenReturn("hashed");
        when(userCommandPort.save(any())).thenAnswer(inv -> {
            CreateUserCommand cmd = inv.getArgument(0);
            return new User(UUID.randomUUID(), cmd.username(), cmd.email(),
                cmd.password(), cmd.role(), true, Instant.now(), null, false);
        });

        User result = handler.register(
            new RegisterCommand("alice", "ALICE@TEST.COM", "password123", Role.USER),
            Role.SUPER_ADMIN
        );

        assertThat(result.email()).isEqualTo("alice@test.com");
    }

    private RegisterCommand cmd(String username, Role role) {
        return new RegisterCommand(username, username + "@test.com", "pass", role);
    }

    private User user(String username, Role role) {
        return new User(UUID.randomUUID(), username, username + "@test.com", "hashed", role, true, Instant.now(), null, false);
    }
}