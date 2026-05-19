package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.application.LoginHandler;
import com.synapsys.api.auth.application.RefreshTokenHandler;
import com.synapsys.api.auth.domain.port.out.AccessTokenPort;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.RefreshTokenIssuerPort;
import com.synapsys.api.auth.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.auth.domain.port.out.TokenHashPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    LoginHandler loginHandler(UserRepository userRepository,
                              PasswordHasherPort passwordHasherPort,
                              AccessTokenPort accessTokenPort,
                              RefreshTokenIssuerPort refreshTokenPort,
                              SynapsysProperties properties) {
        return new LoginHandler(userRepository, passwordHasherPort, accessTokenPort,
            refreshTokenPort, properties.refreshToken().expiryDays());
    }

    @Bean
    RefreshTokenHandler refreshTokenHandler(RefreshTokenRepository refreshTokenRepository,
                                            UserRepository userRepository,
                                            AccessTokenPort accessTokenPort,
                                            RefreshTokenIssuerPort refreshTokenPort,
                                            TokenHashPort tokenHashPort,
                                            SynapsysProperties properties) {
        return new RefreshTokenHandler(refreshTokenRepository, userRepository, accessTokenPort,
            refreshTokenPort, tokenHashPort, properties.refreshToken().expiryDays());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}