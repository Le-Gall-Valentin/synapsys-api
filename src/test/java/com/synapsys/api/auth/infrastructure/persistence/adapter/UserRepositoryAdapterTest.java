package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock UserJpaRepository jpa;
    @InjectMocks UserRepositoryAdapter adapter;

    @Test
    void findByEmail_returnsUserWhenFound() {
        UserEntity entity = buildEntity("alice", "alice@test.com");
        when(jpa.findByEmail("alice@test.com")).thenReturn(Optional.of(entity));

        Optional<User> result = adapter.findByEmail("alice@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("alice@test.com");
        assertThat(result.get().username()).isEqualTo("alice");
    }

    @Test
    void findByEmail_returnsEmptyWhenNotFound() {
        when(jpa.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        Optional<User> result = adapter.findByEmail("unknown@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    void saveTotpSecret_delegatesToJpa() {
        UUID id = UUID.randomUUID();
        adapter.saveTotpSecret(id, "SECRETBASE32==");
        verify(jpa).saveTotpSecretById(id, "SECRETBASE32==");
    }

    @Test
    void enableTotp_delegatesToJpa() {
        UUID id = UUID.randomUUID();
        adapter.enableTotp(id);
        verify(jpa).enableTotpById(id);
    }

    @Test
    void disableTotp_delegatesToJpa() {
        UUID id = UUID.randomUUID();
        adapter.disableTotp(id);
        verify(jpa).disableTotpById(id);
    }

    private UserEntity buildEntity(String username, String email) {
        UserEntity e = new UserEntity();
        e.setUsername(username);
        e.setEmail(email);
        e.setPasswordHash("hash");
        e.setRole(Role.USER);
        return e;
    }
}