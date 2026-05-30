package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.auth.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmTotpHandlerTest {

    @Mock UserRepository userRepository;
    @Mock TotpCodeValidatorPort codeValidator;
    @Mock UserTotpPort userTotpPort;
    @Mock TotpChallengeStorePort challengeStore;

    private ConfirmTotpHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final User userWithSecret = new User(
        userId, "user1", "user1@test.com", "hash",
        Role.USER, true, Instant.now(), "SECRETBASE32XXXX", false
    );

    @BeforeEach
    void setUp() {
        handler = new ConfirmTotpHandler(userRepository, codeValidator, userTotpPort, challengeStore);
    }

    @Test
    void confirm_validCode_enablesTotp() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithSecret));
        when(codeValidator.isValid("SECRETBASE32XXXX", "123456")).thenReturn(true);
        when(challengeStore.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);

        assertThatNoException().isThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")));

        verify(userTotpPort).enableTotp(userId);
    }

    @Test
    void confirm_invalidCode_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithSecret));
        when(codeValidator.isValid("SECRETBASE32XXXX", "000000")).thenReturn(false);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "000000")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);

        verifyNoInteractions(userTotpPort);
    }

    @Test
    void confirm_replayedCode_throwsTotpCodeInvalid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithSecret));
        when(codeValidator.isValid("SECRETBASE32XXXX", "123456")).thenReturn(true);
        when(challengeStore.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);

        verifyNoInteractions(userTotpPort);
    }

    @Test
    void confirm_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(AuthException.UserNotFound.class);
    }

    @Test
    void confirm_setupNeverStarted_throwsTotpSetupNotStarted() {
        // setup() was never called — secret is null, but totpEnabled is also false.
        // This is a precondition failure, not a "TOTP not enabled" state.
        User noSecret = new User(userId, "user1", "user1@test.com", "hash",
            Role.USER, true, Instant.now(), null, false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(noSecret));

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(AuthException.TotpSetupNotStarted.class);
    }
}