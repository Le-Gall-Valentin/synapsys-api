package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.RefreshTokenUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class RefreshTokenHandler implements RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenHandler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRevocationPort revocationPort;
    private final UserRepository userRepository;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final TokenHashPort tokenHashPort;
    private final int refreshTokenExpiryDays;

    public RefreshTokenHandler(RefreshTokenRepository refreshTokenRepository,
                               RefreshTokenRevocationPort revocationPort,
                               UserRepository userRepository,
                               AccessTokenPort accessTokenPort,
                               RefreshTokenIssuerPort refreshTokenPort,
                               TokenHashPort tokenHashPort,
                               RefreshTokenConfigPort tokenConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revocationPort = revocationPort;
        this.userRepository = userRepository;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.tokenHashPort = tokenHashPort;
        this.refreshTokenExpiryDays = tokenConfig.refreshTokenExpiryDays();
    }

    @Override
    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException.TokenNotFound();
        }

        String hash = tokenHashPort.hash(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(AuthException.TokenNotFound::new);

        if (token.isRevoked()) {
            log.warn("Revoked token reuse detected for user: {} — revoking all tokens", token.userId());
            try {
                revocationPort.revokeAllForUser(token.userId());
            } catch (Exception e) {
                log.error("Failed to revoke all tokens for user: {}", token.userId(), e);
            }
            throw new AuthException.TokenRevoked();
        }

        if (token.isExpired()) {
            throw new AuthException.TokenExpired();
        }

        User user = userRepository.findById(token.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (!user.isActive()) {
            log.warn("Refresh token presented for inactive account: {}", user.id());
            throw new AuthException.UserNotActive();
        }

        if (!revocationPort.tryMarkUsedAndRevoke(token.id())) {
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