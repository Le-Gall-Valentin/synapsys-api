package com.synapsys.api.auth.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import com.synapsys.api.auth.infrastructure.security.CustomUserDetails;
import com.synapsys.api.auth.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.synapsys.api.IntegrationTestConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class TotpControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserJpaRepository userJpaRepository;
    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired @Qualifier("totpSecretEncryptor") TextEncryptor encryptor;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    // A known Base32 secret that TOTP apps would accept. The exact value does not matter for
    // tests that only check HTTP responses without verifying a real code.
    private static final String KNOWN_TOTP_SECRET = "JBSWY3DPEHPK3PXP";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        refreshTokenJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // Plain user — no TOTP
        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setPasswordHash(encoder.encode("password"));
        user.setRole(Role.USER);
        userJpaRepository.save(user);

        // Super-admin (for admin-only endpoints)
        UserEntity admin = new UserEntity();
        admin.setUsername("superadmin");
        admin.setEmail("superadmin@test.com");
        admin.setPasswordHash(encoder.encode("adminpass"));
        admin.setRole(Role.SUPER_ADMIN);
        userJpaRepository.save(admin);

        // User with TOTP already enabled
        UserEntity totpUser = new UserEntity();
        totpUser.setUsername("totpuser");
        totpUser.setEmail("totpuser@test.com");
        totpUser.setPasswordHash(encoder.encode("totppass"));
        totpUser.setRole(Role.USER);
        totpUser.setTotpSecret(encryptor.encrypt(KNOWN_TOTP_SECRET));
        totpUser.setTotpEnabled(true);
        userJpaRepository.save(totpUser);
    }

    // ─── /api/auth/2fa/setup ──────────────────────────────────────────────────

    @Test
    void setup_authenticated_returns200WithOtpauthUriAndSecret() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.otpauthUri").value(startsWith("otpauth://totp/")))
            .andExpect(jsonPath("$.secret").isNotEmpty());
    }

    @Test
    void setup_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/2fa/setup"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void setup_alreadyEnabled_returns409() throws Exception {
        // totpuser has totpEnabled=true; logging in as them starts the challenge flow (no access_token).
        // Inject authentication directly to reach the endpoint as an authenticated totpuser.
        String totpUserId = userJpaRepository.findByUsername("totpuser")
            .orElseThrow().getId().toString();
        CustomUserDetails totpPrincipal = new CustomUserDetails(
            java.util.UUID.fromString(totpUserId), Role.USER);

        mockMvc.perform(post("/api/auth/2fa/setup")
                .with(user(totpPrincipal)))
            .andExpect(status().isConflict());
    }

    // ─── /api/auth/2fa/confirm ────────────────────────────────────────────────

    @Test
    void confirm_invalidCode_returns401() throws Exception {
        Cookie access = loginAs("testuser", "password");

        // First call setup so a pending secret exists
        mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/2fa/confirm")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ─── /api/auth/2fa/verify ─────────────────────────────────────────────────

    @Test
    void verify_withValidChallengeAndWrongCode_returns401() throws Exception {
        Cookie challenge = loginAndGetChallengeCookie("totpuser", "totppass");

        mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void verify_withoutChallengeCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/2fa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void verify_challengeLocksAfter5FailedAttempts() throws Exception {
        Cookie challenge = loginAndGetChallengeCookie("totpuser", "totppass");

        // First 4 attempts should return 401 (TotpCodeInvalid → "Authentication required")
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/2fa/verify")
                    .cookie(challenge)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"00000" + i + "\"}"))
                .andExpect(status().isUnauthorized());
        }

        // 5th attempt exhausts the counter, invalidates the challenge, and returns TotpMaxAttemptsExceeded
        mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000005\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("TotpMaxAttemptsExceeded"));
    }

    // ─── /api/auth/2fa (DELETE) ───────────────────────────────────────────────

    @Test
    void disable_totpNotEnabled_returns409() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(delete("/api/auth/2fa")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void disable_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/auth/2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ─── Login with TOTP required ─────────────────────────────────────────────

    @Test
    void login_totpRequired_setsTotpChallengeCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("totpuser", "totppass"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpRequired").value(true))
            .andExpect(header().string("Set-Cookie", containsString("totp_challenge")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth/2fa")));
    }

    // ─── /api/auth/2fa/verify — happy path ───────────────────────────────────

    @Test
    void verify_withValidCode_returns200AndSetsJwtCookies() throws Exception {
        Cookie challenge = loginAndGetChallengeCookie("totpuser", "totppass");

        // Generate the current-window TOTP code for the known test secret using SHA-256
        DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        long counter = Math.floorDiv(System.currentTimeMillis() / 1000L, 30);
        String validCode = generator.generate(KNOWN_TOTP_SECRET, counter);

        MvcResult result = mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + validCode + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("totpuser"))
            .andExpect(jsonPath("$.totpEnabled").value(true))
            .andReturn();

        // Verify JWT cookies are set (multiple Set-Cookie headers — check response cookies directly)
        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }

    private Cookie loginAndGetChallengeCookie(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("totp_challenge");
    }
}