package com.synapsys.api.agent.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.TestHashUtils;
import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import com.synapsys.api.agent.infrastructure.persistence.repository.EnrollmentTokenJpaRepository;
import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.authentication.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.synapsys.api.shared.model.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class EnrollmentTokenControllerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext wac;
    @Autowired EnrollmentTokenJpaRepository tokenRepository;
    @Autowired UserIdentityJpaRepository userRepository;
    @Autowired UserCredentialJpaRepository credentialRepository;
    @Autowired RedisRateLimitBucketStore bucketStore;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private UUID adminId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        bucketStore.clearAll();
        tokenRepository.deleteAll();
        credentialRepository.deleteAll();
        userRepository.deleteAll();
        adminId = seedUser("superadmin", "adminpass", Role.SUPER_ADMIN);
        seedUser("plainuser", "userpass", Role.USER);
    }

    private UUID seedUser(String username, String password, Role role) {
        UserIdentityEntity u = new UserIdentityEntity();
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setRole(role);
        UUID id = userRepository.saveAndFlush(u).getId();
        UserCredentialEntity c = new UserCredentialEntity();
        c.setUserId(id);
        c.setPasswordHash(encoder.encode(password));
        credentialRepository.save(c);
        return id;
    }

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return r.getResponse().getCookie("access_token");
    }

    @Test
    void create_asSuperAdmin_returns201_withClearTokenAndLocation() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        mockMvc.perform(post("/api/agents/enrollment-tokens").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serverName\":\"web-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/agents/enrollment-tokens/[0-9a-f-]+")))
            .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.startsWith("synenr_")))
            .andExpect(jsonPath("$.serverName").value("web-01"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_asUser_returns403() throws Exception {
        Cookie access = loginAs("plainuser", "userpass");
        mockMvc.perform(post("/api/agents/enrollment-tokens").cookie(access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"serverName\":\"web-01\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/agents/enrollment-tokens")
                .contentType(MediaType.APPLICATION_JSON).content("{\"serverName\":\"web-01\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void create_blankServerName_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        mockMvc.perform(post("/api/agents/enrollment-tokens").cookie(access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"serverName\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_withTtlMinutes_setsShorterExpiry() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        Instant before = Instant.now();
        MvcResult result = mockMvc.perform(post("/api/agents/enrollment-tokens").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serverName\":\"web-01\",\"ttlMinutes\":15}"))
            .andExpect(status().isCreated())
            .andReturn();
        String expiresAt = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("expiresAt").asText();
        Instant exp = Instant.parse(expiresAt);
        org.assertj.core.api.Assertions.assertThat(exp)
            .isAfter(before.plus(14, ChronoUnit.MINUTES))
            .isBefore(before.plus(16, ChronoUnit.MINUTES));
    }

    @Test
    void create_ttlAboveMaximum_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        mockMvc.perform(post("/api/agents/enrollment-tokens").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serverName\":\"web-01\",\"ttlMinutes\":1500}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_ttlBelowOne_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        mockMvc.perform(post("/api/agents/enrollment-tokens").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serverName\":\"web-01\",\"ttlMinutes\":0}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void list_neverLeaksTokenOrHash() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        tokenRepository.saveAndFlush(new EnrollmentTokenEntity(
            "web-01", TestHashUtils.sha256("synenr_raw"), Instant.now().plus(24, ChronoUnit.HOURS), adminId));
        mockMvc.perform(get("/api/agents/enrollment-tokens").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].serverName").value("web-01"))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.content[0].createdBy.id").value(adminId.toString()))
            .andExpect(jsonPath("$.content[0].createdBy.username").value("superadmin"))
            .andExpect(jsonPath("$.content[0].token").doesNotExist())
            .andExpect(jsonPath("$.content[0].tokenHash").doesNotExist());
    }

    @Test
    void list_asUser_returns403() throws Exception {
        Cookie access = loginAs("plainuser", "userpass");
        mockMvc.perform(get("/api/agents/enrollment-tokens").cookie(access)).andExpect(status().isForbidden());
    }

    @Test
    void revoke_activeToken_returns204() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        EnrollmentTokenEntity t = tokenRepository.saveAndFlush(new EnrollmentTokenEntity(
            "web-01", TestHashUtils.sha256("synenr_raw2"), Instant.now().plus(24, ChronoUnit.HOURS), adminId));
        mockMvc.perform(post("/api/agents/enrollment-tokens/" + t.getId() + "/revoke").cookie(access))
            .andExpect(status().isNoContent());
    }

    @Test
    void revoke_consumedToken_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        EnrollmentTokenEntity t = new EnrollmentTokenEntity(
            "web-01", TestHashUtils.sha256("synenr_raw3"), Instant.now().plus(24, ChronoUnit.HOURS), adminId);
        t.setConsumedAt(Instant.now());
        tokenRepository.saveAndFlush(t);
        mockMvc.perform(post("/api/agents/enrollment-tokens/" + t.getId() + "/revoke").cookie(access))
            .andExpect(status().isConflict());
    }

    @Test
    void revoke_nonExistent_returns404() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        mockMvc.perform(post("/api/agents/enrollment-tokens/" + UUID.randomUUID() + "/revoke").cookie(access))
            .andExpect(status().isNotFound());
    }

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/agents/enrollment-tokens"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void revoke_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/agents/enrollment-tokens/" + java.util.UUID.randomUUID() + "/revoke"))
            .andExpect(status().isUnauthorized());
    }
}
