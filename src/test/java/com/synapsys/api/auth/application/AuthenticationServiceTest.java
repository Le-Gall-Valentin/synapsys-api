package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.out.AccessTokenPort;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.synapsys.api.TestHashUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordHasherPort passwordHasher;
    @Mock AccessTokenPort accessTokenPort;

    private AuthenticationService service;

    private final User activeUser = new User(
        UUID.randomUUID(), "user1", "user1@test.com",
        "hashed_pw", Role.USER, true, Instant.now()
    );

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(
            userRepository, refreshTokenRepository,
            passwordHasher, accessTokenPort,
            new AuthConfig(30)
        );
    }

    @Test
    void login_success_returnsTokensAndSavesRefreshToken() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches("password", "hashed_pw")).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("jwt_access");

        AuthTokens result = service.login(new LoginCommand("user1", "password"));

        assertThat(result.accessToken()).isEqualTo("jwt_access");
        assertThat(result.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthException.InvalidCredentials.class);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("user1", "wrong")))
            .isInstanceOf(AuthException.InvalidCredentials.class);
    }

    @Test
    void login_inactiveUser_throwsUserNotActive() {
        User inactive = new User(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), activeUser.role(), false, activeUser.createdAt());
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthException.UserNotActive.class);
    }

    @Test
    void refresh_validToken_rotatesAndReturnsNewTokens() {
        String raw = "valid-raw-token";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(stored));
        when(userRepository.findById(activeUser.id())).thenReturn(Optional.of(activeUser));
        when(accessTokenPort.generate(activeUser)).thenReturn("new_jwt");

        AuthTokens result = service.refresh(raw);

        assertThat(result.accessToken()).isEqualTo("new_jwt");
        verify(refreshTokenRepository).revoke(stored.id());
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void refresh_unknownToken_throwsTokenExpired() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("unknown"))
            .isInstanceOf(AuthException.TokenExpired.class);
    }

    @Test
    void refresh_nullToken_throwsTokenExpired() {
        assertThatThrownBy(() -> service.refresh(null))
            .isInstanceOf(AuthException.TokenExpired.class);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void refresh_revokedToken_revokesAllForUserAndThrowsTokenRevoked() {
        String raw = "stolen-token";
        RefreshToken revoked = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), sha256(raw),
            Instant.now().plusSeconds(3600), true, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh(raw))
            .isInstanceOf(AuthException.TokenRevoked.class);
        verify(refreshTokenRepository).revokeAllForUser(activeUser.id());
    }

    @Test
    void refresh_expiredToken_throwsTokenExpired() {
        String raw = "expired-token";
        RefreshToken expired = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), sha256(raw),
            Instant.now().minusSeconds(1), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh(raw))
            .isInstanceOf(AuthException.TokenExpired.class);
    }

    @Test
    void logout_validToken_revokesIt() {
        String raw = "valid-token";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(stored));

        service.logout(raw);

        verify(refreshTokenRepository).revoke(stored.id());
    }

    @Test
    void logout_unknownToken_isIdempotent() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> service.logout("unknown"));
        verify(refreshTokenRepository, never()).revoke(any());
    }

    @Test
    void logout_nullToken_isIdempotent() {
        assertThatNoException().isThrownBy(() -> service.logout(null));
        verifyNoInteractions(refreshTokenRepository);
    }

    private String sha256(String raw) {
        return TestHashUtils.sha256(raw);
    }
}