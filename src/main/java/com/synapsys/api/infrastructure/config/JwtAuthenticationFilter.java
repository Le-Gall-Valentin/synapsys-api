package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.infrastructure.security.CookieService;
import com.synapsys.api.auth.infrastructure.security.JwtService;
import com.synapsys.api.auth.infrastructure.security.UserClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CookieService cookieService;

    public JwtAuthenticationFilter(JwtService jwtService, CookieService cookieService) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE).ifPresent(token -> {
            try {
                UserClaims claims = jwtService.validateAndExtract(token);
                var auth = new UsernamePasswordAuthenticationToken(
                    claims.userId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                log.debug("Invalid JWT token: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("Unexpected error during JWT validation", e);
            }
        });

        chain.doFilter(request, response);
    }
}