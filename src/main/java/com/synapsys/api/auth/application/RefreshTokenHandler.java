package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.RefreshTokenUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@ApplicationService
public class RefreshTokenHandler implements RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenHandler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final TokenHashPort tokenHashPort;
    private final int refreshTokenExpiryDays;

    public RefreshTokenHandler(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               AccessTokenPort accessTokenPort,
                               RefreshTokenIssuerPort refreshTokenPort,
                               TokenHashPort tokenHashPort,
                               SynapsysProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.tokenHashPort = tokenHashPort;
        this.refreshTokenExpiryDays = properties.refreshToken().expiryDays();
    }

    @Override
    @Transactional(noRollbackFor = AuthException.TokenRevoked.class)
    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException.TokenExpired();
        }

        String hash = tokenHashPort.hash(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(AuthException.TokenExpired::new);

        if (token.expiresAt().isBefore(Instant.now())) {
            throw new AuthException.TokenExpired();
        }

        if (token.revoked()) {
            log.warn("Revoked token reuse detected for user: {} — revoking all tokens", token.userId());
            refreshTokenRepository.revokeAllForUser(token.userId());
            throw new AuthException.TokenRevoked();
        }

        User user = userRepository.findById(token.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (!user.isActive()) {
            log.warn("Refresh token presented for inactive account: {}", user.id());
            throw new AuthException.UserNotActive();
        }

        if (!refreshTokenRepository.tryMarkUsedAndRevoke(token.id())) {
            // Token was concurrently consumed by another request — treat as reuse
            log.warn("Concurrent token reuse detected for user: {}", user.id());
            throw new AuthException.TokenRevoked();
        }

        log.info("Token rotated for user: {}", user.id());
        return new AuthTokens(
            accessTokenPort.generate(user),
            refreshTokenPort.generate(user, refreshTokenExpiryDays)
        );
    }
}