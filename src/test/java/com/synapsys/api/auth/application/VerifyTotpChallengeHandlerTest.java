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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyTotpChallengeHandlerTest {

    @Mock TotpChallengeStorePort challengeStore;
    @Mock TotpCodeValidatorPort codeValidator;
    @Mock UserRepository userRepository;
    @Mock AccessTokenPort accessTokenPort;
    @Mock RefreshTokenIssuerPort refreshTokenPort;
    @Mock RefreshTokenConfigPort tokenConfig;

    private VerifyTotpChallengeHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final User user = new User(
        userId, "user1", "user1@test.com", "hash",
        Role.USER, true, Instant.now(), "SECRETBASE32XXXX", true
    );

    @BeforeEach
    void setUp() {
        when(tokenConfig.refreshTokenExpiryDays()).thenReturn(30);
        handler = new VerifyTotpChallengeHandler(
            challengeStore, codeValidator, userRepository,
            accessTokenPort, refreshTokenPort, tokenConfig
        );
    }

    @Test
    void verify_validChallenge_validCode_returnsSuccess() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(codeValidator.isValid("SECRETBASE32XXXX", "123456")).thenReturn(true);
        when(challengeStore.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);
        when(accessTokenPort.generate(user)).thenReturn("jwt-token");
        when(refreshTokenPort.generate(eq(user), anyInt())).thenReturn("refresh-token");

        LoginResult.Success result = handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456"));

        assertThat(result.tokens().accessToken()).isEqualTo("jwt-token");
        assertThat(result.tokens().refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user()).isEqualTo(user);
    }

    @Test
    void verify_validChallenge_validCode_invalidatesChallenge() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(codeValidator.isValid("SECRETBASE32XXXX", "123456")).thenReturn(true);
        when(challengeStore.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);
        when(accessTokenPort.generate(any())).thenReturn("jwt");
        when(refreshTokenPort.generate(any(), anyInt())).thenReturn("refresh");

        handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456"));

        verify(challengeStore).invalidateChallenge("challenge-id");
        verify(challengeStore).markCodeUsedIfAbsent(userId, "123456");
    }

    @Test
    void verify_expiredChallenge_throwsTotpChallengeExpired() {
        when(challengeStore.resolveChallenge("expired-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("expired-id", "123456")))
            .isInstanceOf(AuthException.TotpChallengeExpired.class);
    }

    @Test
    void verify_invalidCode_throwsTotpCodeInvalid_withoutConsumingCode() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(codeValidator.isValid("SECRETBASE32XXXX", "000000")).thenReturn(false);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "000000")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);

        // Wrong code must NOT be marked as used — user can retry with next window's code
        verify(challengeStore, never()).markCodeUsedIfAbsent(any(), any());
    }

    @Test
    void verify_replayedCode_throwsTotpCodeInvalid() {
        // Valid code but SETNX returns false → another request already consumed it
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(codeValidator.isValid("SECRETBASE32XXXX", "123456")).thenReturn(true);
        when(challengeStore.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);
    }

    @Test
    void verify_inactiveUser_throwsUserNotActive() {
        User inactive = new User(userId, "user1", "user1@test.com", "hash",
            Role.USER, false, Instant.now(), "SECRET", true);
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456")))
            .isInstanceOf(AuthException.UserNotActive.class);
    }

    @Test
    void verify_invalidCode_incrementsAttempts() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(codeValidator.isValid("SECRETBASE32XXXX", "000000")).thenReturn(false);
        when(challengeStore.incrementFailedAttempts("challenge-id")).thenReturn(1);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "000000")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);

        verify(challengeStore).incrementFailedAttempts("challenge-id");
    }

    @Test
    void verify_invalidCode_atMaxAttempts_throwsTotpChallengeExpiredAndInvalidates() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(codeValidator.isValid("SECRETBASE32XXXX", "000000")).thenReturn(false);
        when(challengeStore.incrementFailedAttempts("challenge-id")).thenReturn(5);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "000000")))
            .isInstanceOf(AuthException.TotpChallengeExpired.class);

        verify(challengeStore).invalidateChallenge("challenge-id");
    }

    @Test
    void verify_invalidCode_belowMaxAttempts_doesNotInvalidateChallenge() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(codeValidator.isValid("SECRETBASE32XXXX", "000000")).thenReturn(false);
        when(challengeStore.incrementFailedAttempts("challenge-id")).thenReturn(4);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "000000")))
            .isInstanceOf(AuthException.TotpCodeInvalid.class);

        verify(challengeStore, never()).invalidateChallenge(any());
    }
}