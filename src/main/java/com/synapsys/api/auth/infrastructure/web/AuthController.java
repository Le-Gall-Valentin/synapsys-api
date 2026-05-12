package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.AuthTokens;
import com.synapsys.api.auth.domain.model.LoginCommand;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.in.*;
import com.synapsys.api.auth.infrastructure.security.CookieService;
import com.synapsys.api.auth.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.auth.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final CookieService cookieService;
    private final SynapsysProperties properties;

    public AuthController(LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase,
                          CookieService cookieService,
                          SynapsysProperties properties) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.cookieService = cookieService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletResponse response) {
        AuthTokens tokens = loginUseCase.login(new LoginCommand(request.username(), request.password()));
        addAuthCookies(response, tokens);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = getCurrentUserUseCase.getCurrentUser(userId);
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.role()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request,
                                        HttpServletResponse response) {
        String rawRefreshToken = cookieService
            .extractFromRequest(request, CookieService.REFRESH_COOKIE)
            .orElse(null);
        AuthTokens tokens = refreshTokenUseCase.refresh(rawRefreshToken);
        addAuthCookies(response, tokens);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        String rawRefreshToken = cookieService
            .extractFromRequest(request, CookieService.REFRESH_COOKIE)
            .orElse(null);
        logoutUseCase.logout(rawRefreshToken);
        cookieService.buildClearCookies().forEach(response::addCookie);
        return ResponseEntity.noContent().build();
    }

    private void addAuthCookies(HttpServletResponse response, AuthTokens tokens) {
        int accessMaxAge = properties.jwt().expiryMinutes() * 60;
        int refreshMaxAge = properties.refreshToken().expiryDays() * 86_400;
        response.addCookie(cookieService.buildAccessCookie(tokens.accessToken(), accessMaxAge));
        response.addCookie(cookieService.buildRefreshCookie(tokens.refreshToken(), refreshMaxAge));
    }
}