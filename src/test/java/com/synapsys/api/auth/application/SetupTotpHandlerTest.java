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
    void setup_generatesSecretAndStoresIt_whenNoSecretExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(secretGenerator.generateSecret()).thenReturn("NEWBASE32SECRET=");
        when(userTotpPort.saveTotpSecretIfAbsent(userId, "NEWBASE32SECRET=")).thenReturn(true);
        when(secretGenerator.buildOtpauthUri("NEWBASE32SECRET=", "user1@test.com"))
            .thenReturn("otpauth://totp/...");

        TotpSetupResult result = handler.setup(new SetupTotpCommand(userId));

        assertThat(result.secret()).isEqualTo("NEWBASE32SECRET=");
        assertThat(result.otpauthUri()).isEqualTo("otpauth://totp/...");
        verify(userTotpPort).saveTotpSecretIfAbsent(userId, "NEWBASE32SECRET=");
    }

    @Test
    void setup_concurrentRequest_returnsExistingSecret() {
        // Another concurrent request already saved a secret (saveTotpSecretIfAbsent returns false).
        // This request must return the secret already in DB so both clients get the same QR code.
        User userAfterConcurrentWrite = new User(
            userId, "user1", "user1@test.com", "hash",
            Role.USER, true, Instant.now(), "WINNER_SECRET====", false
        );
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user))                      // first read: no secret
            .thenReturn(Optional.of(userAfterConcurrentWrite)); // reload after race lost
        when(secretGenerator.generateSecret()).thenReturn("LOSER_SECRET=====");
        when(userTotpPort.saveTotpSecretIfAbsent(userId, "LOSER_SECRET=====")).thenReturn(false);
        when(secretGenerator.buildOtpauthUri("WINNER_SECRET====", "user1@test.com"))
            .thenReturn("otpauth://totp/winner");

        TotpSetupResult result = handler.setup(new SetupTotpCommand(userId));

        assertThat(result.secret()).isEqualTo("WINNER_SECRET====");
        assertThat(result.otpauthUri()).isEqualTo("otpauth://totp/winner");
    }

    @Test
    void setup_withPendingSecret_returnsExistingSecret() {
        // setup() called again while totpEnabled=false and a previous secret exists.
        // Idempotent: return the existing secret rather than generating a new one,
        // preventing the user from scanning a stale QR if they had already started setup.
        User pendingUser = new User(userId, "user1", "user1@test.com", "hash",
            Role.USER, true, Instant.now(), "PENDINGSECRET===", false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));
        when(secretGenerator.generateSecret()).thenReturn("NEWSECRET=======");
        when(userTotpPort.saveTotpSecretIfAbsent(userId, "NEWSECRET=======")).thenReturn(false);
        // Reload: still has the pending secret (no concurrent write here, just DB already had one)
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));
        when(secretGenerator.buildOtpauthUri("PENDINGSECRET===", "user1@test.com"))
            .thenReturn("otpauth://totp/pending");

        TotpSetupResult result = handler.setup(new SetupTotpCommand(userId));

        assertThat(result.secret()).isEqualTo("PENDINGSECRET===");
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
}