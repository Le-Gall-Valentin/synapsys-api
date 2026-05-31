package com.synapsys.api.authentication.infrastructure.web;

import com.synapsys.api.authentication.application.dto.VerifyTotpChallengeCommand;
import com.synapsys.api.authentication.application.port.in.VerifyTotpChallengeUseCase;
import com.synapsys.api.authentication.domain.model.AuthenticationException;
import com.synapsys.api.authentication.domain.model.LoginResult;
import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.infrastructure.security.CookieService;
import com.synapsys.api.authentication.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa")
public class TotpChallengeController {

    private final VerifyTotpChallengeUseCase verifyUseCase;
    private final CookieService cookieService;

    public TotpChallengeController(VerifyTotpChallengeUseCase verifyUseCase,
                                   CookieService cookieService) {
        this.verifyUseCase = verifyUseCase;
        this.cookieService = cookieService;
    }

    @PostMapping("/verify")
    @RateLimiting(max = 10)
    public ResponseEntity<UserInfoResponse> verify(@Valid @RequestBody VerifyRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse response) {
        String challengeId = cookieService
            .extractFromRequest(httpRequest, CookieService.TOTP_CHALLENGE_COOKIE)
            .orElseThrow(AuthenticationException.TotpChallengeExpired::new);

        LoginResult.Success result = verifyUseCase.verify(
            new VerifyTotpChallengeCommand(challengeId, request.code())
        );

        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildAccessCookie(result.tokens().accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildRefreshCookie(result.tokens().refreshToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildClearChallengeCookie().toString());

        UserCredentials user = result.user();
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.role()));
    }

    record VerifyRequest(@NotBlank String code) {}
}