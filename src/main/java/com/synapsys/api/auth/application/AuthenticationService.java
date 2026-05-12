package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.*;
import com.synapsys.api.auth.domain.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthenticationService
    implements LoginUseCase, RefreshTokenUseCase, LogoutUseCase, GetCurrentUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final AuthConfig authConfig;

    public AuthenticationService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordHasherPort passwordHasher,
        AccessTokenPort accessTokenPort,
        AuthConfig authConfig
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
        this.authConfig = authConfig;
    }

    @Override
    public AuthTokens login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
            .orElseThrow(AuthException.InvalidCredentials::new);

        if (!user.isActive()) {
            throw new AuthException.UserNotActive();
        }

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new AuthException.InvalidCredentials();
        }

        return createSession(user);
    }

    @Override
    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException.TokenExpired();
        }
        String hash = sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(AuthException.TokenExpired::new);

        if (token.revoked()) {
            refreshTokenRepository.revokeAllForUser(token.userId());
            throw new AuthException.TokenRevoked();
        }

        if (token.expiresAt().isBefore(Instant.now())) {
            throw new AuthException.TokenExpired();
        }

        refreshTokenRepository.revoke(token.id());

        User user = userRepository.findById(token.userId())
            .orElseThrow(AuthException.InvalidCredentials::new);

        return createSession(user);
    }

    @Override
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        String hash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
            .ifPresent(token -> refreshTokenRepository.revoke(token.id()));
    }

    @Override
    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(AuthException.InvalidCredentials::new);
    }

    private AuthTokens createSession(User user) {
        String rawRefreshToken = UUID.randomUUID().toString();
        Instant now = Instant.now();

        RefreshToken refreshToken = new RefreshToken(
            null,
            user.id(),
            sha256(rawRefreshToken),
            now.plusSeconds(authConfig.refreshTokenExpiryDays() * 86400L),
            false,
            now,
            null
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthTokens(accessTokenPort.generate(user), rawRefreshToken);
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}