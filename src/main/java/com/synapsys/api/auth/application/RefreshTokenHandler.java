package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.RefreshTokenUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

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
                               int refreshTokenExpiryDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.tokenHashPort = tokenHashPort;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
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

        User user = userRepository.findById(token.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (!user.isActive()) {
            log.warn("Refresh token presented for inactive account: {}", user.id());
            throw new AuthException.UserNotActive();
        }

        refreshTokenRepository.markUsedAndRevoke(token.id());

        log.info("Token rotated for user: {}", user.id());
        return new AuthTokens(
            accessTokenPort.generate(user),
            refreshTokenPort.generate(user, refreshTokenExpiryDays)
        );
    }
}