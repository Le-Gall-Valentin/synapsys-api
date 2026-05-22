package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.port.in.SeedUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock SeedUseCase seedUseCase;

    @Test
    void seed_withoutPassword_throws() {
        DataSeeder seeder = new DataSeeder(properties(" "), seedUseCase);
        assertThatThrownBy(seeder::seed)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SYNAPSYS_SEED_PASSWORD must be set");
        verifyNoInteractions(seedUseCase);
    }

    @Test
    void seed_withPassword_delegatesToSeedUseCase() {
        DataSeeder seeder = new DataSeeder(properties("secret"), seedUseCase);
        assertThatNoException().isThrownBy(seeder::seed);
        verify(seedUseCase).seedInitialSuperAdmin("user", "user@test.com", "secret");
    }

    private SynapsysProperties properties(String password) {
        return new SynapsysProperties(
            new SynapsysProperties.JwtProperties("test-secret-key-at-least-32-chars", 15),
            new SynapsysProperties.RefreshTokenProperties(30),
            new SynapsysProperties.CookieProperties(false),
            new SynapsysProperties.SeedProperties("user", "user@test.com", password),
            new SynapsysProperties.CorsProperties(List.of()),
            new SynapsysProperties.RateLimitProperties(List.of())
        );
    }
}