package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.infrastructure.security.JwtKeyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;

@Configuration
public class AppConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecretKey jwtSecretKey(SynapsysProperties properties) {
        return JwtKeyFactory.from(properties.jwt().secret());
    }
}