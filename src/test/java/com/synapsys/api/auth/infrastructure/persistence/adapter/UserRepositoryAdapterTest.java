package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock UserJpaRepository jpa;
    @Mock TextEncryptor encryptor;

    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserRepositoryAdapter(jpa, encryptor);
    }

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
    void toDomain_decryptsNonNullTotpSecret() {
        UserEntity entity = buildEntity("bob", "bob@test.com");
        entity.setTotpSecret("encrypted-secret");
        when(jpa.findByEmail("bob@test.com")).thenReturn(Optional.of(entity));
        when(encryptor.decrypt("encrypted-secret")).thenReturn("PLAINTEXT32CHARS");

        Optional<User> result = adapter.findByEmail("bob@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().totpSecret()).isEqualTo("PLAINTEXT32CHARS");
        verify(encryptor).decrypt("encrypted-secret");
    }

    @Test
    void toDomain_nullTotpSecret_doesNotDecrypt() {
        UserEntity entity = buildEntity("carol", "carol@test.com");
        // totpSecret is null by default
        when(jpa.findByEmail("carol@test.com")).thenReturn(Optional.of(entity));

        adapter.findByEmail("carol@test.com");

        verifyNoInteractions(encryptor);
    }

    @Test
    void saveTotpSecret_encryptsBeforeStoring() {
        UUID id = UUID.randomUUID();
        when(encryptor.encrypt("PLAINBASE32==")).thenReturn("encrypted-value");

        adapter.saveTotpSecret(id, "PLAINBASE32==");

        verify(jpa).saveTotpSecretById(id, "encrypted-value");
    }

    @Test
    void saveTotpSecretIfAbsent_encryptsAndReturnsTrue_whenRowUpdated() {
        UUID id = UUID.randomUUID();
        when(encryptor.encrypt("PLAIN_SECRET====")).thenReturn("enc-value");
        when(jpa.saveTotpSecretIfAbsent(id, "enc-value")).thenReturn(1);

        boolean result = adapter.saveTotpSecretIfAbsent(id, "PLAIN_SECRET====");

        assertThat(result).isTrue();
        verify(jpa).saveTotpSecretIfAbsent(id, "enc-value");
    }

    @Test
    void saveTotpSecretIfAbsent_returnsFalse_whenNoRowUpdated() {
        UUID id = UUID.randomUUID();
        when(encryptor.encrypt("PLAIN_SECRET====")).thenReturn("enc-value");
        when(jpa.saveTotpSecretIfAbsent(id, "enc-value")).thenReturn(0);

        boolean result = adapter.saveTotpSecretIfAbsent(id, "PLAIN_SECRET====");

        assertThat(result).isFalse();
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