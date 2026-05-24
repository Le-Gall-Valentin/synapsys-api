package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.*;
import com.synapsys.api.auth.infrastructure.security.CookieService;
import com.synapsys.api.auth.infrastructure.security.CustomUserDetails;
import com.synapsys.api.auth.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.auth.infrastructure.web.dto.RegisterRequest;
import com.synapsys.api.auth.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final RegisterUseCase registerUseCase;
    private final CookieService cookieService;

    public AuthController(LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase,
                          RegisterUseCase registerUseCase,
                          CookieService cookieService) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.registerUseCase = registerUseCase;
        this.cookieService = cookieService;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/register")
    public ResponseEntity<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     @AuthenticationPrincipal CustomUserDetails caller) {
        Role targetRole = request.role() != null ? request.role() : Role.USER;
        User user = registerUseCase.register(
            new RegisterCommand(request.username(), request.email(), request.password(), targetRole),
            caller.getRole()
        );
        return ResponseEntity.status(201).body(
            new UserInfoResponse(user.id(), user.username(), user.role())
        );
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

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal CustomUserDetails caller) {
        User user = getCurrentUserUseCase.getCurrentUser(caller.getUserId());
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.role()));
    }

    @PostMapping("/refresh")
    @RateLimiting(max = 5)
    public ResponseEntity<Void> refresh(HttpServletRequest request,
                                        HttpServletResponse response) {
        String rawRefreshToken = cookieService
            .extractFromRequest(request, CookieService.REFRESH_COOKIE)
            .orElse(null);
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