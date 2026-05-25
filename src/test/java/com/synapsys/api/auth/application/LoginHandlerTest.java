package com.synapsys.api.auth.application;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.out.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

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
    @Mock RefreshTokenConfigPort tokenConfig;

    private LoginHandler handler;

    private final User activeUser = new User(
        UUID.randomUUID(), "user1", "user1@test.com",
        "hashed_pw", Role.USER, true, Instant.now()
    );

    @AfterEach
    void tearDown() {
        // Retire l'appender pour éviter une fuite mémoire entre les tests
        ch.qos.logback.classic.Logger loginLogger =
            (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(LoginHandler.class);
        loginLogger.detachAndStopAllAppenders();
    }

    @BeforeEach
    void setUp() {
        // Constructor calls passwordHasher.hash() to precompute the dummy hash — stub it first
        lenient().when(passwordHasher.hash(anyString())).thenReturn("$2a$12$stubbed-dummy-hash-for-tests");
        when(tokenConfig.refreshTokenExpiryDays()).thenReturn(30);
        handler = new LoginHandler(userRepository, passwordHasher, accessTokenPort, refreshTokenPort, tokenConfig);
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
        when(passwordHasher.matches("password", inactive.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthException.UserNotActive.class);
    }

    @Test
    void login_inactiveUser_doesNotLogUsername() {
        User inactive = new User(activeUser.id(), "alice", activeUser.email(),
            activeUser.passwordHash(), activeUser.role(), false, activeUser.createdAt());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(inactive));
        when(passwordHasher.matches("password", inactive.passwordHash())).thenReturn(true);
        ListAppender<ILoggingEvent> logs = startLogCapture();

        assertThatThrownBy(() -> handler.login(new LoginCommand("alice", "password")))
            .isInstanceOf(AuthException.UserNotActive.class);

        boolean usernameLogged = logs.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("alice"));
        assertThat(usernameLogged).isFalse();
    }

    @Test
    void login_wrongPassword_doesNotLogUsername() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(
            new User(activeUser.id(), "bob", activeUser.email(),
                activeUser.passwordHash(), activeUser.role(), true, activeUser.createdAt())
        ));
        when(passwordHasher.matches("wrong", activeUser.passwordHash())).thenReturn(false);
        ListAppender<ILoggingEvent> logs = startLogCapture();

        assertThatThrownBy(() -> handler.login(new LoginCommand("bob", "wrong")))
            .isInstanceOf(AuthException.InvalidCredentials.class);

        boolean usernameLogged = logs.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("bob"));
        assertThat(usernameLogged).isFalse();
    }

    @Test
    void login_inactiveUser_correctPassword_throwsUserNotActive() {
        User inactive = new User(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), activeUser.role(), false, activeUser.createdAt());
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(inactive));
        when(passwordHasher.matches("correctpassword", inactive.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "correctpassword")))
            .isInstanceOf(AuthException.UserNotActive.class);
    }

    @Test
    void login_inactiveUser_wrongPassword_throwsInvalidCredentials() {
        // Password is checked before isActive to prevent account status info disclosure.
        // An attacker who doesn't know the password must not learn the account exists and is inactive.
        User inactive = new User(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), activeUser.role(), false, activeUser.createdAt());
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(inactive));
        when(passwordHasher.matches("wrongpassword", inactive.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "wrongpassword")))
            .isInstanceOf(AuthException.InvalidCredentials.class);
    }

    private ListAppender<ILoggingEvent> startLogCapture() {
        ch.qos.logback.classic.Logger logger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LoginHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}