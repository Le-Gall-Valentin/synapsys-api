package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "test-secret-key-that-is-at-least-32-chars-long!";

    @BeforeEach
    void setUp() {
        var properties = new SynapsysProperties(
            new SynapsysProperties.JwtProperties(SECRET, 15),
            new SynapsysProperties.RefreshTokenProperties(30),
            new SynapsysProperties.CookieProperties(false),
            null,
            new SynapsysProperties.CorsProperties(java.util.List.of()),
            new SynapsysProperties.RateLimitProperties(java.util.List.of())
        );
        jwtService = new JwtService(properties);
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
        UserClaims claims = jwtService.validateAndExtract(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void validateAndExtract_throwsOnGarbage() {
        assertThatThrownBy(() -> jwtService.validateAndExtract("not.a.jwt"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateAndExtract_throwsOnTamperedToken() {
        User user = new User(UUID.randomUUID(), "alice", "alice@test.com",
            "hash", Role.USER, true, Instant.now());
        String token = jwtService.generate(user) + "tampered";

        assertThatThrownBy(() -> jwtService.validateAndExtract(token))
            .isInstanceOf(IllegalArgumentException.class);
    }
}