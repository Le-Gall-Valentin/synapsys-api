package com.synapsys.api.auth.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import com.synapsys.api.auth.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.infrastructure.ratelimit.CaffeineRateLimitBucketStore;
import com.synapsys.api.IntegrationTestConfig;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class UserControllerIT {

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
    void me_withValidAccessToken_returnsUserInfo() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void me_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withTamperedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .cookie(new Cookie("access_token", "eyJhbGciOiJIUzI1NiJ9.tampered.signature")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_asSuperAdmin_createsUser() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"Securepass1!\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/users/[0-9a-f-]+")))
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"Securepass1!\",\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void register_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"securepass\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"other@test.com\",\"password\":\"Securepass1!\",\"role\":\"USER\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void register_withoutRole_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"brandnewuser\",\"email\":\"brand@test.com\",\"password\":\"Securepass1!\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"simpleuser\",\"email\":\"simple@test.com\",\"password\":\"simplepass\",\"role\":\"USER\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_superAdminCannotCreateSuperAdmin_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newsa\",\"email\":\"newsa@test.com\",\"password\":\"Securepass1!\",\"role\":\"SUPER_ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deactivate_asSuperAdmin_deletesUser_returns204() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        String targetId = userJpaRepository.findByUsername("testuser").get().getId().toString();

        mockMvc.perform(delete("/api/users/" + targetId).cookie(access))
            .andExpect(status().isNoContent());
    }

    @Test
    void deactivate_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        String targetId = userJpaRepository.findByUsername("superadmin").get().getId().toString();

        mockMvc.perform(delete("/api/users/" + targetId).cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void deactivate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/users/" + java.util.UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivate_nonExistentUser_returns404() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(delete("/api/users/" + java.util.UUID.randomUUID()).cookie(access))
            .andExpect(status().isNotFound());
    }

    @Test
    void deactivate_self_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        String selfId = userJpaRepository.findByUsername("superadmin").get().getId().toString();

        mockMvc.perform(delete("/api/users/" + selfId).cookie(access))
            .andExpect(status().isForbidden());
    }

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }
}