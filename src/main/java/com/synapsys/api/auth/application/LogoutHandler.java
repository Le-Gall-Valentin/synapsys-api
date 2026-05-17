package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.port.in.LogoutUseCase;
import com.synapsys.api.auth.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.auth.domain.port.out.TokenHashPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationService
public class LogoutHandler implements LogoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutHandler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashPort tokenHashPort;

    public LogoutHandler(RefreshTokenRepository refreshTokenRepository,
                         TokenHashPort tokenHashPort) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashPort = tokenHashPort;
    }

    @Override
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        String hash = tokenHashPort.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
            .ifPresent(token -> {
                refreshTokenRepository.revoke(token.id());
                log.info("Token revoked on logout for user: {}", token.userId());
            });
    }
}