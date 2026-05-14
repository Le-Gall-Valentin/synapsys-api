package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.application.AuthConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    AuthConfig authConfig(SynapsysProperties properties) {
        return new AuthConfig(properties.refreshToken().expiryDays());
    }
}