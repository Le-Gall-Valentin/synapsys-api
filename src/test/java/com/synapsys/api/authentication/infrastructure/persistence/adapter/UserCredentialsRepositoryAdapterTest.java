package com.synapsys.api.authentication.infrastructure.persistence.adapter;

import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.identity.application.service.UserCredentialsService;
import com.synapsys.api.mfa.application.service.TotpStatusService;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialsRepositoryAdapterTest {

    @Mock UserCredentialsService identityUserService;
    @Mock UserCredentialJpaRepository credentialRepo;
    @Mock TotpStatusService totpStatusService;

    private UserCredentialsRepositoryAdapter adapter;

    private final UUID userId = UUID.randomUUID();
    private final UserCredentialsService.UserInfo userInfo = new UserCredentialsService.UserInfo(
        userId, "alice", "alice@test.com", true, Role.USER, Instant.now()
    );

    @BeforeEach
    void setUp() {
        adapter = new UserCredentialsRepositoryAdapter(identityUserService, credentialRepo, totpStatusService);
    }

    private UserCredentialEntity entityWithHash(String hash) {
        UserCredentialEntity entity = new UserCredentialEntity();
        entity.setPasswordHash(hash);
        return entity;
    }

    @Test
    void findByUsername_userFound_credentialFound_returnsUserCredentials() {
        when(identityUserService.findByUsername("alice")).thenReturn(Optional.of(userInfo));
        when(credentialRepo.findById(userId)).thenReturn(Optional.of(entityWithHash("hashed_pw")));
        when(totpStatusService.isTotpEnabled(userId)).thenReturn(false);

        Optional<UserCredentials> result = adapter.findByUsername("alice");

        assertThat(result).isPresent();
        UserCredentials creds = result.get();
        assertThat(creds.id()).isEqualTo(userId);
        assertThat(creds.username()).isEqualTo("alice");
        assertThat(creds.email()).isEqualTo("alice@test.com");
        assertThat(creds.passwordHash()).isEqualTo("hashed_pw");
        assertThat(creds.isActive()).isTrue();
        assertThat(creds.role()).isEqualTo(Role.USER);
        assertThat(creds.totpEnabled()).isFalse();
    }

    @Test
    void findByUsername_userNotFound_returnsEmpty() {
        when(identityUserService.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findByUsername("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUsername_credentialNotFound_returnsEmpty() {
        when(identityUserService.findByUsername("alice")).thenReturn(Optional.of(userInfo));
        when(credentialRepo.findById(userId)).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findByUsername("alice");

        assertThat(result).isEmpty();
    }

    @Test
    void findById_userFound_totpEnabled_returnsUserCredentialsWithTotpTrue() {
        when(identityUserService.findById(userId)).thenReturn(Optional.of(userInfo));
        when(credentialRepo.findById(userId)).thenReturn(Optional.of(entityWithHash("hashed_pw")));
        when(totpStatusService.isTotpEnabled(userId)).thenReturn(true);

        Optional<UserCredentials> result = adapter.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().totpEnabled()).isTrue();
        assertThat(result.get().id()).isEqualTo(userId);
        assertThat(result.get().passwordHash()).isEqualTo("hashed_pw");
    }

    @Test
    void findById_userNotFound_returnsEmpty() {
        when(identityUserService.findById(userId)).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findById(userId);

        assertThat(result).isEmpty();
    }
}