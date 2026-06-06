package com.synapsys.api.authentication.application.handler;

import com.synapsys.api.authentication.domain.model.AuthenticationException;
import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.domain.port.out.PasswordHasherPort;
import com.synapsys.api.authentication.domain.port.out.PasswordVerifierPort;
import com.synapsys.api.authentication.domain.port.out.UserCredentialPort;
import com.synapsys.api.authentication.domain.port.out.UserCredentialsPort;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordHandlerTest {

    @Mock UserCredentialsPort userCredentialsPort;
    @Mock PasswordVerifierPort passwordVerifier;
    @Mock PasswordHasherPort passwordHasher;
    @Mock UserCredentialPort userCredentialPort;

    private ChangePasswordHandler handler;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ChangePasswordHandler(userCredentialsPort, passwordVerifier, passwordHasher, userCredentialPort);
    }

    @Test
    void changePassword_correctCurrentPassword_updatesHash() {
        UserCredentials creds = new UserCredentials(userId, "user", "u@test.com", "$hashed", true, Role.USER, Instant.now());
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(passwordVerifier.matches("oldPass", "$hashed")).thenReturn(true);
        when(passwordHasher.hash("newPass")).thenReturn("$newHashed");

        assertThatCode(() -> handler.changePassword(userId, "oldPass", "newPass"))
            .doesNotThrowAnyException();

        verify(userCredentialPort).updatePasswordHash(userId, "$newHashed");
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidCurrentPassword() {
        UserCredentials creds = new UserCredentials(userId, "user", "u@test.com", "$hashed", true, Role.USER, Instant.now());
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(passwordVerifier.matches("wrongPass", "$hashed")).thenReturn(false);

        assertThatThrownBy(() -> handler.changePassword(userId, "wrongPass", "newPass"))
            .isInstanceOf(AuthenticationException.InvalidCurrentPassword.class);

        verify(userCredentialPort, never()).updatePasswordHash(any(), any());
    }

    @Test
    void changePassword_credentialsNotFound_throwsDataIntegrityError() {
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.changePassword(userId, "any", "any"))
            .isInstanceOf(AuthenticationException.DataIntegrityError.class);

        verify(userCredentialPort, never()).updatePasswordHash(any(), any());
    }

}