package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.application.AuthConfig;
import com.synapsys.api.auth.application.AuthenticationService;
import com.synapsys.api.auth.domain.port.out.AccessTokenPort;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    AuthConfig authConfig(SynapsysProperties properties) {
        return new AuthConfig(properties.refreshToken().expiryDays());
    }

    @Bean
    AuthenticationService authenticationService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordHasherPort passwordHasher,
        AccessTokenPort accessTokenPort,
        AuthConfig authConfig
    ) {
        return new AuthenticationService(
            userRepository, refreshTokenRepository, passwordHasher, accessTokenPort, authConfig
        );
    }
}