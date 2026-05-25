package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtAuthenticationFilter jwtFilter,
                                    SynapsysProperties properties) throws Exception {
        return http
            // CSRF disabled: all state-changing requests require a valid HttpOnly cookie (SameSite=Strict).
            // Cross-origin requests are blocked by CORS. No CSRF token header needed.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setContentType("application/problem+json");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Authentication required\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setContentType("application/problem+json");
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"Insufficient permissions\"}");
                })
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .build();
    }

    private CorsConfigurationSource corsConfigurationSource(SynapsysProperties properties) {
        List<String> allowedOrigins = properties.cors() != null
            ? properties.cors().allowedOrigins().stream().filter(s -> !s.isBlank()).toList()
            : List.of();
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins.isEmpty()) {
            // Production: SPA served from same origin — no cross-origin requests needed
            config.setAllowedOrigins(List.of());
        } else {
            config.setAllowedOrigins(allowedOrigins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}