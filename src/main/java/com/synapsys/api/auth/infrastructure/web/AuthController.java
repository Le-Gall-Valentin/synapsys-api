package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.*;
import com.synapsys.api.auth.infrastructure.security.CookieService;
import com.synapsys.api.auth.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.auth.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final CookieService cookieService;

    public AuthController(LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          CookieService cookieService) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.cookieService = cookieService;
    }

    @PostMapping("/login")
    @RateLimiting(max = 5)
    public ResponseEntity<UserInfoResponse> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletResponse response) {
        LoginResult result = loginUseCase.login(new LoginCommand(request.username(), request.password()));
        response.addCookie(cookieService.buildAccessCookie(result.tokens().accessToken()));
        response.addCookie(cookieService.buildRefreshCookie(result.tokens().refreshToken()));
        User user = result.user();
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.role()));
    }

    @PostMapping("/refresh")
    @RateLimiting(max = 5)
    public ResponseEntity<Void> refresh(HttpServletRequest request,
                                        HttpServletResponse response) {
        String rawRefreshToken = cookieService
            .extractFromRequest(request, CookieService.REFRESH_COOKIE)
            .orElseThrow(AuthException.TokenNotFound::new);
        var tokens = refreshTokenUseCase.refresh(rawRefreshToken);
        response.addCookie(cookieService.buildAccessCookie(tokens.accessToken()));
        response.addCookie(cookieService.buildRefreshCookie(tokens.refreshToken()));
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
}