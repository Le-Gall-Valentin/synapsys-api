package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.LoginUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

@ApplicationService
public class LoginHandler implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final TokenHashPort tokenHashPort;
    private final AuthConfig authConfig;

    public LoginHandler(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordHasherPort passwordHasher,
                        AccessTokenPort accessTokenPort,
                        TokenHashPort tokenHashPort,
                        AuthConfig authConfig) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
        this.tokenHashPort = tokenHashPort;
        this.authConfig = authConfig;
    }

    @Override
    public AuthTokens login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
            .orElseThrow(() -> {
                log.warn("Login attempt for unknown username: {}", command.username());
                return new AuthException.InvalidCredentials();
            });

        if (!user.isActive()) {
            log.warn("Login attempt on inactive account: {}", command.username());
            throw new AuthException.UserNotActive();
        }

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            log.warn("Failed login attempt for username: {}", command.username());
            throw new AuthException.InvalidCredentials();
        }

        log.info("Successful login for user: {}", user.id());
        return createSession(user);
    }

    AuthTokens createSession(User user) {
        String rawRefreshToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        RefreshToken refreshToken = new RefreshToken(
            null, user.id(), tokenHashPort.hash(rawRefreshToken),
            now.plusSeconds(authConfig.refreshTokenExpiryDays() * 86400L),
            false, now, null
        );
        refreshTokenRepository.save(refreshToken);
        return new AuthTokens(accessTokenPort.generate(user), rawRefreshToken);
    }
}