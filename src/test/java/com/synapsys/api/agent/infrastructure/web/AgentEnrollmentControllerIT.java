package com.synapsys.api.agent.infrastructure.web;

import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.TestHashUtils;
import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import com.synapsys.api.agent.infrastructure.persistence.repository.AgentJpaRepository;
import com.synapsys.api.agent.infrastructure.persistence.repository.EnrollmentTokenJpaRepository;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class AgentEnrollmentControllerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext wac;
    @Autowired EnrollmentTokenJpaRepository tokenRepository;
    @Autowired AgentJpaRepository agentRepository;
    @Autowired UserIdentityJpaRepository userRepository;
    @Autowired RedisRateLimitBucketStore bucketStore;

    private MockMvc mockMvc;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        bucketStore.clearAll();
        agentRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        UserIdentityEntity u = new UserIdentityEntity();
        u.setUsername("enroll-admin");
        u.setEmail("enroll-admin@test.com");
        u.setRole(Role.ADMIN);
        adminId = userRepository.saveAndFlush(u).getId();
    }

    private void seedToken(String rawToken, String serverName, Instant expiresAt, boolean consumed) {
        EnrollmentTokenEntity e = new EnrollmentTokenEntity(serverName, TestHashUtils.sha256(rawToken), expiresAt, adminId);
        if (consumed) e.setConsumedAt(Instant.now());
        tokenRepository.saveAndFlush(e);
    }

    private static String key(int seed) {
        byte[] pk = new byte[32];
        pk[0] = (byte) seed;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(pk);
    }

    private String body(String token, String publicKey) {
        return "{\"token\":\"" + token + "\",\"publicKey\":\"" + publicKey + "\"}";
    }

    @Test
    void enroll_validToken_returns201_pendingAgent() throws Exception {
        seedToken("synenr_ok", "web-01", Instant.now().plus(24, ChronoUnit.HOURS), false);
        mockMvc.perform(post("/api/agents/enroll").contentType(MediaType.APPLICATION_JSON)
                .content(body("synenr_ok", key(1))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.serverName").value("web-01"))
            .andExpect(jsonPath("$.fingerprint").isNotEmpty())
            .andExpect(jsonPath("$.websocketPath").value("/ws/agents"));
    }

    @Test
    void enroll_unknownToken_returns422() throws Exception {
        mockMvc.perform(post("/api/agents/enroll").contentType(MediaType.APPLICATION_JSON)
                .content(body("synenr_missing", key(2))))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void enroll_consumedToken_returns422() throws Exception {
        seedToken("synenr_used", "web-02", Instant.now().plus(24, ChronoUnit.HOURS), true);
        mockMvc.perform(post("/api/agents/enroll").contentType(MediaType.APPLICATION_JSON)
                .content(body("synenr_used", key(3))))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void enroll_badKeyLength_returns400() throws Exception {
        seedToken("synenr_badkey", "web-03", Instant.now().plus(24, ChronoUnit.HOURS), false);
        String shortKey = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[31]);
        mockMvc.perform(post("/api/agents/enroll").contentType(MediaType.APPLICATION_JSON)
                .content(body("synenr_badkey", shortKey)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void enroll_duplicateKey_returns409() throws Exception {
        seedToken("synenr_a", "web-04", Instant.now().plus(24, ChronoUnit.HOURS), false);
        seedToken("synenr_b", "web-05", Instant.now().plus(24, ChronoUnit.HOURS), false);
        mockMvc.perform(post("/api/agents/enroll").contentType(MediaType.APPLICATION_JSON)
                .content(body("synenr_a", key(7)))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/agents/enroll").contentType(MediaType.APPLICATION_JSON)
                .content(body("synenr_b", key(7)))).andExpect(status().isConflict());
    }

    @Test
    void enroll_rateLimited_after5Attempts_returns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/agents/enroll").contentType(MediaType.APPLICATION_JSON)
                    .content(body("synenr_missing", key(2))))
                .andExpect(status().isUnprocessableEntity());
        }
        mockMvc.perform(post("/api/agents/enroll").contentType(MediaType.APPLICATION_JSON)
                .content(body("synenr_missing", key(2))))
            .andExpect(status().isTooManyRequests());
    }
}
