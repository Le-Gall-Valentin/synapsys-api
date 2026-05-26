package com.synapsys.api.auth.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import com.synapsys.api.auth.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.infrastructure.ratelimit.CaffeineRateLimitBucketStore;
import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.TestHashUtils;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class AuthControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserJpaRepository userJpaRepository;
    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired CaffeineRateLimitBucketStore rateLimitBucketStore;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        refreshTokenJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setPasswordHash(encoder.encode("password"));
        user.setRole(Role.USER);
        userJpaRepository.save(user);

        UserEntity admin = new UserEntity();
        admin.setUsername("superadmin");
        admin.setEmail("superadmin@test.com");
        admin.setPasswordHash(encoder.encode("adminpass"));
        admin.setRole(Role.SUPER_ADMIN);
        userJpaRepository.save(admin);
    }

    @Test
    void login_success_returnUserInfoAndSetsTwoHttpOnlyCookies() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn();

        Cookie access = result.getResponse().getCookie("access_token");
        Cookie refresh = result.getResponse().getCookie("refresh_token");

        assertThat(access).isNotNull();
        assertThat(access.isHttpOnly()).isTrue();
        assertThat(access.getPath()).isEqualTo("/api");

        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/auth");
    }

    @Test
    void login_success_setsRateLimitHeaders() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password"))))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"))
            .andExpect(header().exists("X-RateLimit-Reset"))
            .andExpect(header().doesNotExist("Retry-After"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "wrongpassword"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_validToken_rotatesAndSetsNewCookies() throws Exception {
        Cookie firstRefresh = loginAndGetCookie("refresh_token");

        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
            .andExpect(status().isNoContent())
            .andReturn();

        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();

        assertThat(refreshTokenJpaRepository.findByTokenHash(sha256(firstRefresh.getValue())))
            .isPresent()
            .get()
            .extracting(RefreshTokenEntity::isRevoked)
            .isEqualTo(true);
    }

    @Test
    void refresh_withNoRefreshCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_revokedToken_revokesAllAndReturns401() throws Exception {
        Cookie firstRefresh = loginAndGetCookie("refresh_token");

        mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
            .andExpect(status().isUnauthorized());

        assertThat(refreshTokenJpaRepository.findAll())
            .allMatch(RefreshTokenEntity::isRevoked);
    }

    @Test
    void logout_revokesTokenAndClearsCookies() throws Exception {
        MvcResult loginResult = login();
        Cookie access = loginResult.getResponse().getCookie("access_token");
        Cookie refresh = loginResult.getResponse().getCookie("refresh_token");

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                .cookie(access).cookie(refresh))
            .andExpect(status().isNoContent())
            .andReturn();

        Cookie clearedAccess = logoutResult.getResponse().getCookie("access_token");
        Cookie clearedRefresh = logoutResult.getResponse().getCookie("refresh_token");
        assertThat(clearedAccess).isNotNull();
        assertThat(clearedAccess.getMaxAge()).isZero();
        assertThat(clearedRefresh).isNotNull();
        assertThat(clearedRefresh.getMaxAge()).isZero();

        assertThat(refreshTokenJpaRepository.findByTokenHash(sha256(refresh.getValue())))
            .isPresent()
            .get()
            .extracting(RefreshTokenEntity::isRevoked)
            .isEqualTo(true);
    }

    @Test
    void unauthenticatedRequest_returns401WithProblemDetailFormat() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void login_whenIpRateLimitExceeded_returns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .with(req -> { req.setRemoteAddr("198.51.100.1"); return req; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "wrongpass"))))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                .with(req -> { req.setRemoteAddr("198.51.100.1"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "wrongpass"))))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("X-RateLimit-Limit", "5"))
            .andExpect(header().string("X-RateLimit-Remaining", "0"))
            .andExpect(header().exists("X-RateLimit-Reset"))
            .andExpect(header().exists("Retry-After"));
    }

    @Test
    void refresh_whenIpRateLimitExceeded_returns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/refresh")
                    .with(req -> { req.setRemoteAddr("198.51.100.2"); return req; }))
                .andExpect(status().isUnauthorized()); // pas de cookie refresh → 401
        }

        mockMvc.perform(post("/api/auth/refresh")
                .with(req -> { req.setRemoteAddr("198.51.100.2"); return req; }))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));
    }

    MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password"))))
            .andReturn();
    }

    Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }

    Cookie loginAndGetCookie(String name) throws Exception {
        return login().getResponse().getCookie(name);
    }

    String sha256(String raw) {
        return TestHashUtils.sha256(raw);
    }
}