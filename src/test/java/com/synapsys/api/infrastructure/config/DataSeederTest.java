package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasherPort passwordHasher;

    @Test
    void seed_withoutPassword_throws() {
        DataSeeder seeder = new DataSeeder(properties(" "), userRepository, passwordHasher);
        assertThatThrownBy(seeder::seed)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SYNAPSYS_SEED_PASSWORD must be set");
        verifyNoInteractions(userRepository, passwordHasher);
    }

    @Test
    void seed_duplicateUser_isIdempotent() {
        User existing = new User(UUID.randomUUID(), "user", "user@test.com", "hash", Role.SUPER_ADMIN, true, Instant.now());
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(existing));
        DataSeeder seeder = new DataSeeder(properties("secret"), userRepository, passwordHasher);

        assertThatNoException().isThrownBy(seeder::seed);
        verify(userRepository).findByUsername("user");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordHasher);
    }

    @Test
    void seed_success_savesUser() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(passwordHasher.hash("secret")).thenReturn("hashed");
        DataSeeder seeder = new DataSeeder(properties("secret"), userRepository, passwordHasher);

        seeder.seed();

        verify(passwordHasher).hash("secret");
        verify(userRepository).save(any());
    }

    private SynapsysProperties properties(String password) {
        return new SynapsysProperties(
            new SynapsysProperties.JwtProperties("test-secret-key-at-least-32-chars", 15),
            new SynapsysProperties.RefreshTokenProperties(30),
            new SynapsysProperties.CookieProperties(false),
            new SynapsysProperties.SeedProperties("user", "user@test.com", password)
        );
    }
}