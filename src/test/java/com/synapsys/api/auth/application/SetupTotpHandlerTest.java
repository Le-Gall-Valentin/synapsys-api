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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupTotpHandlerTest {

    @Mock UserRepository userRepository;
    @Mock TotpSecretGeneratorPort secretGenerator;
    @Mock UserTotpPort userTotpPort;

    private SetupTotpHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final User user = new User(
        userId, "user1", "user1@test.com", "hash",
        Role.USER, true, Instant.now(), null, false
    );

    @BeforeEach
    void setUp() {
        handler = new SetupTotpHandler(userRepository, secretGenerator, userTotpPort);
    }

    @Test
    void setup_generatesSecretAndStoresIt() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(secretGenerator.generateSecret()).thenReturn("NEWBASE32SECRET=");
        when(secretGenerator.buildOtpauthUri("NEWBASE32SECRET=", "user1@test.com"))
            .thenReturn("otpauth://totp/...");

        TotpSetupResult result = handler.setup(new SetupTotpCommand(userId));

        assertThat(result.secret()).isEqualTo("NEWBASE32SECRET=");
        assertThat(result.otpauthUri()).isEqualTo("otpauth://totp/...");
        verify(userTotpPort).saveTotpSecret(userId, "NEWBASE32SECRET=");
    }

    @Test
    void setup_totpAlreadyEnabled_throws() {
        User enabled = new User(userId, "user1", "user1@test.com", "hash",
            Role.USER, true, Instant.now(), "EXISTINGSECRET==", true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(enabled));

        assertThatThrownBy(() -> handler.setup(new SetupTotpCommand(userId)))
            .isInstanceOf(AuthException.TotpAlreadyEnabled.class);

        verifyNoInteractions(secretGenerator, userTotpPort);
    }

    @Test
    void setup_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.setup(new SetupTotpCommand(userId)))
            .isInstanceOf(AuthException.UserNotFound.class);
    }

    @Test
    void setup_withPendingSecret_overwritesItWithNewSecret() {
        // setup() called again while totpEnabled=false but a previous secret exists.
        // This is intentional: the old QR code is silently invalidated.
        User pendingUser = new User(userId, "user1", "user1@test.com", "hash",
            Role.USER, true, Instant.now(), "OLDSECRETBASE32=", false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));
        when(secretGenerator.generateSecret()).thenReturn("NEWSECRETBASE32=");
        when(secretGenerator.buildOtpauthUri("NEWSECRETBASE32=", "user1@test.com"))
            .thenReturn("otpauth://totp/new");

        TotpSetupResult result = handler.setup(new SetupTotpCommand(userId));

        assertThat(result.secret()).isEqualTo("NEWSECRETBASE32=");
        verify(userTotpPort).saveTotpSecret(userId, "NEWSECRETBASE32=");
    }
}