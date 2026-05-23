package com.synapsys.api.auth.application;

import com.synapsys.api.TestHashUtils;
import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenHandlerTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserRepository userRepository;
    @Mock AccessTokenPort accessTokenPort;
    @Mock RefreshTokenIssuerPort refreshTokenPort;
    @Mock TokenHashPort tokenHashPort;

    private RefreshTokenHandler handler;

    private final User activeUser = new User(
        UUID.randomUUID(), "user1", "user1@test.com",
        "hashed_pw", Role.USER, true, Instant.now()
    );

    @BeforeEach
    void setUp() {
        var properties = new SynapsysProperties(
            new SynapsysProperties.JwtProperties("test-secret-key-at-least-32-chars!", 15),
            new SynapsysProperties.RefreshTokenProperties(30),
            new SynapsysProperties.CookieProperties(false),
            null,
            new SynapsysProperties.CorsProperties(java.util.List.of()),
            new SynapsysProperties.RateLimitProperties(java.util.List.of())
        );
        handler = new RefreshTokenHandler(
            refreshTokenRepository, userRepository, accessTokenPort, refreshTokenPort, tokenHashPort, properties
        );
        lenient().when(tokenHashPort.hash(any(String.class)))
            .thenAnswer(i -> TestHashUtils.sha256(i.getArgument(0, String.class)));
    }

    @Test
    void refresh_validToken_rotatesAndReturnsNewTokens() {
        String raw = "valid-raw-token";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userRepository.findById(activeUser.id())).thenReturn(Optional.of(activeUser));
        when(refreshTokenRepository.tryMarkUsedAndRevoke(stored.id())).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("new_jwt");
        when(refreshTokenPort.generate(eq(activeUser), anyInt())).thenReturn("new_raw_refresh");

        AuthTokens result = handler.refresh(raw);

        assertThat(result.accessToken()).isEqualTo("new_jwt");
        assertThat(result.refreshToken()).isEqualTo("new_raw_refresh");
        verify(refreshTokenRepository).tryMarkUsedAndRevoke(stored.id());
        verify(refreshTokenPort).generate(activeUser, 30);
    }

    @Test
    void refresh_nullToken_throwsTokenExpired() {
        assertThatThrownBy(() -> handler.refresh(null))
            .isInstanceOf(AuthException.TokenExpired.class);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void refresh_unknownToken_throwsTokenExpired() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.refresh("unknown"))
            .isInstanceOf(AuthException.TokenExpired.class);
    }

    @Test
    void refresh_revokedToken_revokesAllAndThrows() {
        String raw = "stolen";
        RefreshToken revoked = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), true, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthException.TokenRevoked.class);
        verify(refreshTokenRepository).revokeAllForUser(activeUser.id());
    }

    @Test
    void refresh_expiredToken_throwsTokenExpired() {
        String raw = "expired";
        RefreshToken expired = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().minusSeconds(1), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthException.TokenExpired.class);
    }

    @Test
    void refresh_revokedAndExpiredToken_throwsTokenExpired() {
        // Token expired naturally after TTL and was already revoked by prior rotation — not theft
        String raw = "expired-and-revoked";
        RefreshToken expiredRevoked = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().minusSeconds(1), true, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(expiredRevoked));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthException.TokenExpired.class);
        verify(refreshTokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void refresh_deletedUser_throwsUserNotFound() {
        String raw = "valid";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userRepository.findById(activeUser.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthException.UserNotFound.class);
    }

    @Test
    void refresh_concurrentReuse_throwsTokenRevoked() {
        String raw = "valid-concurrent";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userRepository.findById(activeUser.id())).thenReturn(Optional.of(activeUser));
        when(refreshTokenRepository.tryMarkUsedAndRevoke(stored.id())).thenReturn(false);

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthException.TokenRevoked.class);
        verifyNoInteractions(refreshTokenPort);
    }

    @Test
    void refresh_inactiveUser_throwsUserNotActive() {
        String raw = "valid-raw-token";
        User inactiveUser = new User(
            activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), activeUser.role(), false, activeUser.createdAt()
        );
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), inactiveUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userRepository.findById(inactiveUser.id())).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthException.UserNotActive.class);
        verifyNoInteractions(refreshTokenPort);
    }

    @Test
    void refresh_tokenGenerationFails_revokesAllForUserAndRethrows() {
        String raw = "valid-gen-fail";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userRepository.findById(activeUser.id())).thenReturn(Optional.of(activeUser));
        when(refreshTokenRepository.tryMarkUsedAndRevoke(stored.id())).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenThrow(new RuntimeException("JWT infra down"));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JWT infra down");
        verify(refreshTokenRepository).revokeAllForUser(activeUser.id());
        verifyNoInteractions(refreshTokenPort);
    }
}