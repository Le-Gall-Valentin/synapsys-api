package com.synapsys.api.identity.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.authentication.infrastructure.web.dto.LoginRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.synapsys.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.synapsys.api.shared.model.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.synapsys.api.mfa.infrastructure.config.TotpEncryptorFactory;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class UserControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserCredentialJpaRepository userCredentialJpaRepository;
    @Autowired UserIdentityJpaRepository userIdentityJpaRepository;
    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired UserTotpJpaRepository userTotpJpaRepository;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired TotpEncryptorFactory encryptorFactory;

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
        userTotpJpaRepository.deleteAll();
        userCredentialJpaRepository.deleteAll();
        userIdentityJpaRepository.deleteAll();

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setRole(Role.USER);
        userIdentityJpaRepository.saveAndFlush(user);
        saveCredential(user.getId(), "password");

        UserIdentityEntity admin = new UserIdentityEntity();
        admin.setUsername("superadmin");
        admin.setEmail("superadmin@test.com");
        admin.setRole(Role.SUPER_ADMIN);
        userIdentityJpaRepository.saveAndFlush(admin);
        saveCredential(admin.getId(), "adminpass");

        UserIdentityEntity totpUser = new UserIdentityEntity();
        totpUser.setUsername("totpuser");
        totpUser.setEmail("totpuser@test.com");
        totpUser.setRole(Role.USER);
        userIdentityJpaRepository.saveAndFlush(totpUser);
        saveCredential(totpUser.getId(), "totppass");
        saveTotpRecord(totpUser.getId(), "JBSWY3DPEHPK3PXP", true);
    }

    @Test
    void me_withValidAccessToken_returnsUserInfo() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("testuser@test.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.totpEnabled").value(false));
    }

    @Test
    void me_withTotpEnabled_returnsTotpEnabledTrue() throws Exception {
        UserIdentityEntity totpUser = userIdentityJpaRepository.findByUsername("totpuser").get();
        // Build a valid access token directly — loginAs would be blocked by TOTP challenge
        Instant now = Instant.now();
        String token = Jwts.builder()
            .issuer("synapsys-api")
            .audience().add("synapsys-api").and()
            .subject(totpUser.getId().toString())
            .claim("role", totpUser.getRole().name())
            .claim("email", totpUser.getEmail())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(15 * 60L)))
            .signWith(Keys.hmacShaKeyFor(
                "integration-test-secret-at-least-32-chars!".getBytes(StandardCharsets.UTF_8)))
            .compact();
        Cookie access = new Cookie("access_token", token);

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpEnabled").value(true))
            .andExpect(jsonPath("$.username").value("totpuser"))
            .andExpect(jsonPath("$.email").value("totpuser@test.com"));
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
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.email").value("newuser@test.com"))
            .andExpect(jsonPath("$.totpEnabled").value(false))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
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
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(delete("/api/users/" + targetId).cookie(access))
            .andExpect(status().isNoContent());
    }

    @Test
    void deactivate_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID targetId = userIdentityJpaRepository.findByUsername("superadmin").get().getId();

        mockMvc.perform(delete("/api/users/" + targetId).cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void deactivate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/users/" + UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivate_nonExistentUser_returns404() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(delete("/api/users/" + UUID.randomUUID()).cookie(access))
            .andExpect(status().isNotFound());
    }

    @Test
    void deactivate_self_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID selfId = userIdentityJpaRepository.findByUsername("superadmin").get().getId();

        mockMvc.perform(delete("/api/users/" + selfId).cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void resetTotp_asSuperAdmin_resetsTotpForUser_returns204() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("totpuser").get().getId();

        mockMvc.perform(post("/api/users/" + targetId + "/2fa/reset").cookie(access))
            .andExpect(status().isNoContent());
    }

    @Test
    void resetTotp_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID targetId = userIdentityJpaRepository.findByUsername("totpuser").get().getId();

        mockMvc.perform(post("/api/users/" + targetId + "/2fa/reset").cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void resetTotp_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/users/" + UUID.randomUUID() + "/2fa/reset"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void resetTotp_nonExistentUser_returns404() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users/" + UUID.randomUUID() + "/2fa/reset").cookie(access))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_authenticated_updatesUsernameAndEmail_returns204() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"updated\",\"email\":\"updated@test.com\"}"))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findByUsername("updated")).isPresent();
        assertThat(userIdentityJpaRepository.findByEmail("updated@test.com")).isPresent();
    }

    @Test
    void updateProfile_duplicateUsername_returns409() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"superadmin\",\"email\":\"unique@test.com\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateProfile_duplicateEmail_returns409() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"uniqueuser\",\"email\":\"superadmin@test.com\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateProfile_invalidUsername_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ab\",\"email\":\"ok@test.com\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"x\",\"email\":\"x@test.com\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_inactiveUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID testUserId = userIdentityJpaRepository.findByUsername("testuser").get().getId();
        userIdentityJpaRepository.deactivateById(testUserId);

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"updated\",\"email\":\"updated@test.com\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_correctCurrentPassword_returns204() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"Newpassword1!\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns422() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"Newpassword1!\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changePassword_weakNewPassword_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"weak\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"x\",\"newPassword\":\"Newpassword1!\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withInactiveUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID testUserId = userIdentityJpaRepository.findByUsername("testuser").get().getId();
        userIdentityJpaRepository.deactivateById(testUserId);

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateProfile_invalidEmail_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"notanemail\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_sameAsCurrentPassword_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Samepass1!\",\"newPassword\":\"Samepass1!\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_inactiveUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID testUserId = userIdentityJpaRepository.findByUsername("testuser").get().getId();
        userIdentityJpaRepository.deactivateById(testUserId);

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"Newpassword1!\"}"))
            .andExpect(status().isForbidden());
    }

    private void saveCredential(UUID userId, String rawPassword) {
        UserCredentialEntity cred = new UserCredentialEntity();
        cred.setUserId(userId);
        cred.setPasswordHash(encoder.encode(rawPassword));
        userCredentialJpaRepository.save(cred);
    }

    private void saveTotpRecord(UUID userId, String secret, boolean enabled) {
        UserTotpEntity totp = new UserTotpEntity();
        totp.setUserId(userId);
        totp.setTotpSecret(encryptorFactory.forUser(userId).encrypt(secret));
        totp.setTotpEnabled(enabled);
        userTotpJpaRepository.save(totp);
    }

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }
}