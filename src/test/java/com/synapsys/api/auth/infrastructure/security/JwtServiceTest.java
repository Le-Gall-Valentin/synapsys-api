package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtValidationService validationService;

    private static final String SECRET = "test-secret-key-that-is-at-least-32-chars-long!";

    @BeforeEach
    void setUp() {
        SecretKey key = JwtKeyFactory.from(SECRET);
        var properties = new SynapsysProperties(
            new SynapsysProperties.JwtProperties(SECRET, 15),
            new SynapsysProperties.RefreshTokenProperties(30),
            new SynapsysProperties.CookieProperties(false),
            null,
            new SynapsysProperties.CorsProperties(java.util.List.of()),
            new SynapsysProperties.RateLimitProperties(java.util.List.of())
        );
        jwtService = new JwtService(key, properties);
        validationService = new JwtValidationService(key);
    }

    @Test
    void generate_returnsNonBlankToken() {
        User user = new User(UUID.randomUUID(), "alice", "alice@test.com",
            "hash", Role.USER, true, Instant.now());

        String token = jwtService.generate(user);

        assertThat(token).isNotBlank();
    }

    @Test
    void validateAndExtract_returnsCorrectClaims() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice", "alice@test.com",
            "hash", Role.ADMIN, true, Instant.now());

        String token = jwtService.generate(user);
        UserClaims claims = validationService.validateAndExtract(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void validateAndExtract_throwsOnGarbage() {
        assertThatThrownBy(() -> validationService.validateAndExtract("not.a.jwt"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateAndExtract_throwsOnTamperedToken() {
        User user = new User(UUID.randomUUID(), "alice", "alice@test.com",
            "hash", Role.USER, true, Instant.now());
        String token = jwtService.generate(user) + "tampered";

        assertThatThrownBy(() -> validationService.validateAndExtract(token))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateAndExtract_throwsOnTokenWithWrongIssuer() {
        // Token signed with same key but issued by another service — must be rejected
        SecretKey sameKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String foreignToken = Jwts.builder()
            .issuer("other-service")
            .audience().add("other-service").and()
            .subject(UUID.randomUUID().toString())
            .claim("role", Role.USER.name())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(sameKey)
            .compact();

        assertThatThrownBy(() -> validationService.validateAndExtract(foreignToken))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void validateAndExtract_throwsOnExpiredToken() {
        SecretKey key = JwtKeyFactory.from(SECRET);
        var properties = new SynapsysProperties(
            new SynapsysProperties.JwtProperties(SECRET, -1),
            new SynapsysProperties.RefreshTokenProperties(30),
            new SynapsysProperties.CookieProperties(false),
            null,
            new SynapsysProperties.CorsProperties(java.util.List.of()),
            new SynapsysProperties.RateLimitProperties(java.util.List.of())
        );
        JwtService expiredJwtService = new JwtService(key, properties);
        JwtValidationService expiredValidationService = new JwtValidationService(key);
        User user = new User(UUID.randomUUID(), "alice", "alice@test.com",
            "hash", Role.USER, true, Instant.now());

        String token = expiredJwtService.generate(user);

        assertThatThrownBy(() -> expiredValidationService.validateAndExtract(token))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void jwtValidationService_validateAndExtract_returnsCorrectClaims() {
        JwtValidationService vs = new JwtValidationService(JwtKeyFactory.from(SECRET));

        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice", "alice@test.com", "hash", Role.ADMIN, true, Instant.now());
        String token = jwtService.generate(user);

        UserClaims claims = vs.validateAndExtract(token);
        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
    }
}