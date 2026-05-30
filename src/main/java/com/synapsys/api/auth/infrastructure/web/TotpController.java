package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.*;
import com.synapsys.api.auth.infrastructure.security.CookieService;
import com.synapsys.api.auth.infrastructure.security.CustomUserDetails;
import com.synapsys.api.auth.infrastructure.web.dto.*;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa")
public class TotpController {

    private final VerifyTotpChallengeUseCase verifyUseCase;
    private final SetupTotpUseCase setupUseCase;
    private final ConfirmTotpUseCase confirmUseCase;
    private final DisableTotpUseCase disableUseCase;
    private final CookieService cookieService;

    public TotpController(VerifyTotpChallengeUseCase verifyUseCase,
                          SetupTotpUseCase setupUseCase,
                          ConfirmTotpUseCase confirmUseCase,
                          DisableTotpUseCase disableUseCase,
                          CookieService cookieService) {
        this.verifyUseCase = verifyUseCase;
        this.setupUseCase = setupUseCase;
        this.confirmUseCase = confirmUseCase;
        this.disableUseCase = disableUseCase;
        this.cookieService = cookieService;
    }

    @PostMapping("/verify")
    @RateLimiting(max = 5)
    public ResponseEntity<UserInfoResponse> verify(@Valid @RequestBody TotpCodeRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
        String challengeId = cookieService
            .extractFromRequest(httpRequest, CookieService.TOTP_CHALLENGE_COOKIE)
            .orElseThrow(AuthException.TotpChallengeExpired::new);

        LoginResult.Success result = verifyUseCase.verify(
            new VerifyTotpChallengeCommand(challengeId, request.code())
        );

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildClearChallengeCookie().toString());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildAccessCookie(result.tokens().accessToken()).toString());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildRefreshCookie(result.tokens().refreshToken()).toString());

        User user = result.user();
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.role(), user.totpEnabled()));
    }

    @PostMapping("/setup")
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpSetupResponse> setup(@AuthenticationPrincipal CustomUserDetails caller) {
        TotpSetupResult result = setupUseCase.setup(new SetupTotpCommand(caller.getUserId()));
        return ResponseEntity.ok(new TotpSetupResponse(result.otpauthUri(), result.secret()));
    }

    @PostMapping("/confirm")
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirm(@Valid @RequestBody TotpCodeRequest request,
                                        @AuthenticationPrincipal CustomUserDetails caller) {
        confirmUseCase.confirm(new ConfirmTotpCommand(caller.getUserId(), request.code()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disable(@Valid @RequestBody TotpCodeRequest request,
                                        @AuthenticationPrincipal CustomUserDetails caller) {
        disableUseCase.disable(new DisableTotpCommand(caller.getUserId(), request.code()));
        return ResponseEntity.noContent().build();
    }
}