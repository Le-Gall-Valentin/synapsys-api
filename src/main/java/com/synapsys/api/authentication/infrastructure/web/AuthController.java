package com.synapsys.api.authentication.infrastructure.web;

import com.synapsys.api.authentication.application.dto.LoginCommand;
import com.synapsys.api.authentication.application.port.in.LoginUseCase;
import com.synapsys.api.authentication.application.port.in.LogoutUseCase;
import com.synapsys.api.authentication.application.port.in.RefreshTokenUseCase;
import com.synapsys.api.authentication.domain.model.*;
import com.synapsys.api.authentication.infrastructure.security.CookieService;
import com.synapsys.api.authentication.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.authentication.infrastructure.web.dto.TotpRequiredResponse;
import com.synapsys.api.authentication.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletResponse response) {
        LoginResult result = loginUseCase.login(new LoginCommand(request.username(), request.password()));
        return switch (result) {
            case LoginResult.Success s -> {
                response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildAccessCookie(s.tokens().accessToken()).toString());
                response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildRefreshCookie(s.tokens().refreshToken()).toString());
                UserCredentials user = s.credentials();
                yield ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.email(), user.role(), user.createdAt(), false));
            }
            case LoginResult.TotpRequired t -> {
                response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildChallengeCookie(t.challengeId()).toString());
                yield ResponseEntity.ok(new TotpRequiredResponse());
            }
        };
    }

    @PostMapping("/refresh")
    @RateLimiting(max = 5)
    public ResponseEntity<Void> refresh(HttpServletRequest request,
                                        HttpServletResponse response) {
        String rawRefreshToken = cookieService
            .extractFromRequest(request, CookieService.REFRESH_COOKIE)
            .orElseThrow(AuthenticationException.TokenNotFound::new);
        var tokens = refreshTokenUseCase.refresh(rawRefreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildAccessCookie(tokens.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildRefreshCookie(tokens.refreshToken()).toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @RateLimiting(max = 10)
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        String rawRefreshToken = cookieService
            .extractFromRequest(request, CookieService.REFRESH_COOKIE)
            .orElse(null);
        logoutUseCase.logout(rawRefreshToken);
        cookieService.buildClearCookies().forEach(c -> response.addHeader(HttpHeaders.SET_COOKIE, c.toString()));
        return ResponseEntity.noContent().build();
    }
}