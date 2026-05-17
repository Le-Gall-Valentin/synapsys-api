package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.port.in.RegisterUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock RegisterUseCase registerUseCase;

    @Test
    void seed_withoutPassword_throws() {
        DataSeeder seeder = new DataSeeder(properties(" "), registerUseCase);

        assertThatThrownBy(seeder::seed)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SYNAPSYS_SEED_PASSWORD must be set");
    }

    @Test
    void seed_duplicateUser_isIdempotent() {
        DataSeeder seeder = new DataSeeder(properties("secret"), registerUseCase);
        doThrow(new AuthException.UsernameAlreadyExists())
            .when(registerUseCase).register(any());

        assertThatNoException().isThrownBy(seeder::seed);
        verify(registerUseCase).register(any());
    }

    @Test
    void seed_success_registersUser() {
        DataSeeder seeder = new DataSeeder(properties("secret"), registerUseCase);

        seeder.seed();

        verify(registerUseCase).register(any());
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