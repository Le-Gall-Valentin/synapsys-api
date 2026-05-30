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
class DisableTotpHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserTotpPort userTotpPort;
    @Mock TotpCodeValidatorPort codeValidator;
    @Mock TotpChallengeStorePort challengeStore;

    private DisableTotpHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final User enabledUser = new User(
        userId, "user1", "user1@test.com", "hash",
        Role.USER, true, Instant.now(), "SECRETBASE32XXXX", true
    );

    @BeforeEach
    void setUp() {
        handler = new DisableTotpHandler(userRepository, userTotpPort, codeValidator, challengeStore);
    }

    @Test
    void disable_nullSecret_throwsTotpCodeInvalid_withoutNpe() {
        // totpEnabled=true but secret null: data inconsistency — must not call isValid()
        User noSecret = new User(userId, "user1", "user1@test.com", "hash",
            Role.USER, true, Instant.now(), null, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(noSecret));

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);

        verifyNoInteractions(codeValidator);
    }

    @Test
    void disable_validCode_disablesIt() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(enabledUser));
        when(codeValidator.isValid("SECRETBASE32XXXX", "123456")).thenReturn(true);
        when(challengeStore.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);

        assertThatNoException().isThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")));

        verify(userTotpPort).disableTotp(userId);
    }

    @Test
    void disable_replayedCode_throwsTotpCodeInvalid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(enabledUser));
        when(codeValidator.isValid("SECRETBASE32XXXX", "123456")).thenReturn(true);
        when(challengeStore.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);

        verifyNoInteractions(userTotpPort);
    }

    @Test
    void disable_invalidCode_throwsTotpCodeInvalid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(enabledUser));
        when(codeValidator.isValid("SECRETBASE32XXXX", "000000")).thenReturn(false);

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "000000")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);

        verifyNoInteractions(userTotpPort);
    }

    @Test
    void disable_totpNotEnabled_throws() {
        User notEnabled = new User(userId, "user1", "user1@test.com", "hash",
            Role.USER, true, Instant.now(), null, false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(notEnabled));

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .isInstanceOf(AuthException.TotpNotEnabled.class);

        verifyNoInteractions(userTotpPort, codeValidator);
    }

    @Test
    void disable_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .isInstanceOf(AuthException.UserNotFound.class);
    }
}
