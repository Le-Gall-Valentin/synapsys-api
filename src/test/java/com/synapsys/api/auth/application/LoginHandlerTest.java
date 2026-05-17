package com.synapsys.api.auth.application;

import com.synapsys.api.TestHashUtils;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginHandlerTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordHasherPort passwordHasher;
    @Mock AccessTokenPort accessTokenPort;
    @Mock TokenHashPort tokenHashPort;

    private LoginHandler handler;

    private final User activeUser = new User(
        UUID.randomUUID(), "user1", "user1@test.com",
        "hashed_pw", Role.USER, true, Instant.now()
    );

    @BeforeEach
    void setUp() {
        handler = new LoginHandler(
            userRepository, refreshTokenRepository,
            passwordHasher, accessTokenPort, tokenHashPort,
            new AuthConfig(30)
        );
        lenient().when(tokenHashPort.hash(any(String.class)))
            .thenAnswer(i -> TestHashUtils.sha256(i.getArgument(0, String.class)));
    }

    @Test
    void login_success_returnsTokensAndSavesRefreshToken() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches("password", "hashed_pw")).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("jwt_access");

        AuthTokens result = handler.login(new LoginCommand("user1", "password"));

        assertThat(result.accessToken()).isEqualTo("jwt_access");
        assertThat(result.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthException.InvalidCredentials.class);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "wrong")))
            .isInstanceOf(AuthException.InvalidCredentials.class);
    }

    @Test
    void login_inactiveUser_throwsUserNotActive() {
        User inactive = new User(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), activeUser.role(), false, activeUser.createdAt());
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthException.UserNotActive.class);
    }
}