package com.synapsys.api.agent.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.infrastructure.persistence.entity.AgentEntity;
import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import com.synapsys.api.agent.infrastructure.persistence.repository.AgentJpaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class AgentControllerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext wac;
    @Autowired AgentJpaRepository agentRepository;
    @Autowired EnrollmentTokenJpaRepository tokenRepository;
    @Autowired UserIdentityJpaRepository userRepository;
    @Autowired UserCredentialJpaRepository credentialRepository;
    @Autowired RedisRateLimitBucketStore bucketStore;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private UUID adminId;
    private UUID tokenId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        bucketStore.clearAll();
        agentRepository.deleteAll();
        tokenRepository.deleteAll();
        credentialRepository.deleteAll();
        userRepository.deleteAll();
        adminId = seedUser("superadmin", "adminpass", Role.SUPER_ADMIN);
        seedUser("plainuser", "userpass", Role.USER);
        tokenId = tokenRepository.saveAndFlush(new EnrollmentTokenEntity(
            "web-01", "h", Instant.now().plus(24, ChronoUnit.HOURS), adminId)).getId();
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

    private AgentEntity seedAgent(String name, String pk, String fp) {
        return agentRepository.saveAndFlush(new AgentEntity(name, pk, fp, tokenId, adminId));
    }

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password)))).andReturn();
        return r.getResponse().getCookie("access_token");
    }

    @Test
    void list_asSuperAdmin_returnsPendingAgent() throws Exception {
        seedAgent("web-01", "pk-1", "fp-1");
        Cookie access = loginAs("superadmin", "adminpass");
        mockMvc.perform(get("/api/agents").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].serverName").value("web-01"))
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.content[0].fingerprint").value("fp-1"));
    }

    @Test
    void list_asUser_returns403() throws Exception {
        Cookie access = loginAs("plainuser", "userpass");
        mockMvc.perform(get("/api/agents").cookie(access)).andExpect(status().isForbidden());
    }

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/agents")).andExpect(status().isUnauthorized());
    }

    @Test
    void revoke_thenDelete_twoStepLifecycle() throws Exception {
        AgentEntity a = seedAgent("web-01", "pk-1", "fp-1");
        Cookie access = loginAs("superadmin", "adminpass");
        // delete before revoke -> 409
        mockMvc.perform(delete("/api/agents/" + a.getId()).cookie(access)).andExpect(status().isConflict());
        // revoke -> 204
        mockMvc.perform(post("/api/agents/" + a.getId() + "/revoke").cookie(access)).andExpect(status().isNoContent());
        // delete after revoke -> 204
        mockMvc.perform(delete("/api/agents/" + a.getId()).cookie(access)).andExpect(status().isNoContent());
    }

    @Test
    void revoke_alreadyRevoked_returns409() throws Exception {
        AgentEntity a = seedAgent("web-01", "pk-1", "fp-1");
        a.setStatus(AgentLifecycleStatus.REVOKED);
        a.setRevokedAt(Instant.now());
        agentRepository.saveAndFlush(a);
        Cookie access = loginAs("superadmin", "adminpass");
        mockMvc.perform(post("/api/agents/" + a.getId() + "/revoke").cookie(access)).andExpect(status().isConflict());
    }

    @Test
    void revoke_asUser_returns403() throws Exception {
        AgentEntity a = seedAgent("web-01", "pk-1", "fp-1");
        Cookie access = loginAs("plainuser", "userpass");
        mockMvc.perform(post("/api/agents/" + a.getId() + "/revoke").cookie(access)).andExpect(status().isForbidden());
    }

    @Test
    void statistics_asSuperAdmin_countsPending() throws Exception {
        seedAgent("web-01", "pk-1", "fp-1");
        Cookie access = loginAs("superadmin", "adminpass");
        mockMvc.perform(get("/api/agents/statistics").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pending").value(1))
            .andExpect(jsonPath("$.active").value(0))
            .andExpect(jsonPath("$.total").value(1));
    }
}
