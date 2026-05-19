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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginHandlerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasherPort passwordHasher;
    @Mock AccessTokenPort accessTokenPort;
    @Mock RefreshTokenIssuerPort refreshTokenPort;

    private LoginHandler handler;

    private final User activeUser = new User(
        UUID.randomUUID(), "user1", "user1@test.com",
        "hashed_pw", Role.USER, true, Instant.now()
    );

    @BeforeEach
    void setUp() {
        // Constructor calls passwordHasher.hash() to precompute the dummy hash — stub it first
        lenient().when(passwordHasher.hash(anyString())).thenReturn("$2a$12$stubbed-dummy-hash-for-tests");
        handler = new LoginHandler(
            userRepository, passwordHasher, accessTokenPort, refreshTokenPort, 30
        );
    }

    @Test
    void login_success_returnsTokensAndUser() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches("password", "hashed_pw")).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("jwt_access");
        when(refreshTokenPort.generate(eq(activeUser), anyInt())).thenReturn("raw_refresh");

        LoginResult result = handler.login(new LoginCommand("user1", "password"));

        assertThat(result.tokens().accessToken()).isEqualTo("jwt_access");
        assertThat(result.tokens().refreshToken()).isEqualTo("raw_refresh");
        assertThat(result.user()).isEqualTo(activeUser);
        verify(refreshTokenPort).generate(activeUser, 30);
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthException.InvalidCredentials.class);
        verifyNoInteractions(refreshTokenPort);
    }

    @Test
    void login_unknownUser_performsDummyHashComparison() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("unknown", "password")))
            .isInstanceOf(AuthException.InvalidCredentials.class);

        // Dummy comparison must be performed to prevent timing-based username enumeration
        verify(passwordHasher).matches(eq("password"), anyString());
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