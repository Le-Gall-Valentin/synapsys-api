package com.synapsys.api.auth.infrastructure.config;

import com.synapsys.api.auth.infrastructure.security.JwtKeyFactory;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(SynapsysProperties properties) {
        return JwtKeyFactory.from(properties.jwt().secret());
    }
}