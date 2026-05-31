package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.RegisterCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.CredentialSetupPort;
import com.synapsys.api.identity.domain.port.out.TotpRecordInitPort;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterHandlerTest {

    @Mock UserCommandPort userCommandPort;
    @Mock UserRepository userRepository;
    @Mock CredentialSetupPort credentialSetupPort;
    @Mock TotpRecordInitPort totpRecordInitPort;

    private RegisterHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterHandler(userCommandPort, userRepository, credentialSetupPort, totpRecordInitPort);
    }

    @Test
    void register_superAdmin_can_create_admin() {
        UUID userId = UUID.randomUUID();
        User created = user(userId, "newadmin", Role.ADMIN);
        when(userCommandPort.createProfile(any(CreateUserProfileCommand.class))).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(created));

        User result = handler.register(cmd("newadmin", Role.ADMIN), Role.SUPER_ADMIN);

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verify(credentialSetupPort).setup(userId, "pass");
        verify(totpRecordInitPort).initForUser(userId);
    }

    @Test
    void register_superAdmin_can_create_user() {
        UUID userId = UUID.randomUUID();
        User created = user(userId, "newuser", Role.USER);
        when(userCommandPort.createProfile(any())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(created));

        User result = handler.register(cmd("newuser", Role.USER), Role.SUPER_ADMIN);

        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void register_insufficientPermissions_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.register(cmd("sa", Role.SUPER_ADMIN), Role.SUPER_ADMIN))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);
        verifyNoInteractions(userCommandPort);
    }

    @Test
    void register_userCannotCreateAnyone_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.register(cmd("someone", Role.USER), Role.USER))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);
        verifyNoInteractions(userCommandPort);
    }

    @Test
    void register_duplicateUsername_throwsUsernameAlreadyExists() {
        when(userCommandPort.createProfile(any())).thenThrow(new IdentityException.UsernameAlreadyExists());

        assertThatThrownBy(() -> handler.register(cmd("existing", Role.USER), Role.SUPER_ADMIN))
            .isInstanceOf(IdentityException.UsernameAlreadyExists.class);
    }

    private RegisterCommand cmd(String username, Role role) {
        return new RegisterCommand(username, username + "@test.com", "pass", role);
    }

    private User user(UUID id, String username, Role role) {
        return new User(id, username, username + "@test.com", role, true, Instant.now());
    }
}