package com.synapsys.api.auth.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.auth.domain.model.Role;

import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import com.synapsys.api.auth.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.infrastructure.ratelimit.AttemptTracker;
import com.synapsys.api.TestHashUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
    "synapsys.jwt.secret=integration-test-secret-at-least-32-chars!",
    "synapsys.jwt.expiry-minutes=15",
    "synapsys.refresh-token.expiry-days=30",
    "synapsys.cookie.secure=false",
    "synapsys.seed.username=it-admin",
    "synapsys.seed.email=it-admin@test.local",
    "synapsys.seed.password=integration-test-seed-password",
    "synapsys.cors.allowed-origins=",
    "spring.jpa.hibernate.ddl-auto=none"
})
@Testcontainers
class AuthControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserJpaRepository userJpaRepository;
    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired AttemptTracker attemptTracker;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        attemptTracker.clearAll();
        refreshTokenJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setPasswordHash(encoder.encode("password"));
        user.setRole(Role.USER);
        user.setActive(true);
        userJpaRepository.save(user);

        UserEntity admin = new UserEntity();
        admin.setUsername("superadmin");
        admin.setEmail("superadmin@test.com");
        admin.setPasswordHash(encoder.encode("adminpass"));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setActive(true);
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
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "wrong"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidAccessToken_returnsUserInfo() throws Exception {
        Cookie access = loginAndGetCookie("access_token");

        mockMvc.perform(get("/api/auth/me").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void me_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
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
    void register_asSuperAdmin_createsUser() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/auth/register")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"Securepass1!\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_asUser_returns403() throws Exception {
        Cookie access = loginAndGetCookie("access_token");

        mockMvc.perform(post("/api/auth/register")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"Securepass1!\",\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void register_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"securepass\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/auth/register")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"other@test.com\",\"password\":\"Securepass1!\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void register_superAdminCannotCreateSuperAdmin_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/auth/register")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newsa\",\"email\":\"newsa@test.com\",\"password\":\"Securepass1!\",\"role\":\"SUPER_ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password"))))
            .andReturn();
    }

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }

    private Cookie loginAndGetCookie(String name) throws Exception {
        return login().getResponse().getCookie(name);
    }

    @Test
    void login_whenIpRateLimitExceeded_returns429() throws Exception {
        for (int i = 0; i < 10; i++) {
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
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void me_withTamperedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .cookie(new Cookie("access_token", "eyJhbGciOiJIUzI1NiJ9.tampered.signature")))
            .andExpect(status().isUnauthorized());
    }

    private String sha256(String raw) {
        return TestHashUtils.sha256(raw);
    }
}