package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.RefreshTokenUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

@ApplicationService
public class RefreshTokenHandler implements RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenHandler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AccessTokenPort accessTokenPort;
    private final TokenHashPort tokenHashPort;
    private final AuthConfig authConfig;

    public RefreshTokenHandler(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               AccessTokenPort accessTokenPort,
                               TokenHashPort tokenHashPort,
                               AuthConfig authConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.accessTokenPort = accessTokenPort;
        this.tokenHashPort = tokenHashPort;
        this.authConfig = authConfig;
    }

    @Override
    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException.TokenExpired();
        }
        String hash = tokenHashPort.hash(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(AuthException.TokenExpired::new);

        if (token.revoked()) {
            log.warn("Revoked token reuse detected for user: {} — revoking all tokens", token.userId());
            refreshTokenRepository.revokeAllForUser(token.userId());
            throw new AuthException.TokenRevoked();
        }

        if (token.expiresAt().isBefore(Instant.now())) {
            throw new AuthException.TokenExpired();
        }

        refreshTokenRepository.markUsed(token.id());
        refreshTokenRepository.revoke(token.id());

        User user = userRepository.findById(token.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        log.info("Token rotated for user: {}", user.id());
        return createSession(user);
    }

    private AuthTokens createSession(User user) {
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