package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.LoginUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationService
public class LoginHandler implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final int refreshTokenExpiryDays;
    private final String dummyHash;

    public LoginHandler(UserRepository userRepository,
                        PasswordHasherPort passwordHasher,
                        AccessTokenPort accessTokenPort,
                        RefreshTokenIssuerPort refreshTokenPort,
                        SynapsysProperties properties) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.refreshTokenExpiryDays = properties.refreshToken().expiryDays();
        // Precomputed hash for constant-time dummy comparison — prevents timing-based username enumeration
        this.dummyHash = passwordHasher.hash("synapsys-timing-sentinel");
    }

    @Override
    public LoginResult login(LoginCommand command) {
        var userOpt = userRepository.findByUsername(command.username());

        if (userOpt.isEmpty()) {
            passwordHasher.matches(command.password(), dummyHash);
            log.warn("Login attempt for unknown username");
            throw new AuthException.InvalidCredentials();
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            log.warn("Login attempt on inactive account");
            throw new AuthException.UserNotActive();
        }

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            log.warn("Failed login attempt — invalid credentials");
            throw new AuthException.InvalidCredentials();
        }

        log.info("Successful login for user: {}", user.id());
        AuthTokens tokens = new AuthTokens(
            accessTokenPort.generate(user),
            refreshTokenPort.generate(user, refreshTokenExpiryDays)
        );
        return new LoginResult(tokens, user);
    }
}