package com.synapsys.api.mfa.infrastructure.web;

import com.synapsys.api.auth.infrastructure.security.CustomUserDetails;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import com.synapsys.api.mfa.application.port.in.ConfirmTotpUseCase;
import com.synapsys.api.mfa.application.port.in.DisableTotpUseCase;
import com.synapsys.api.mfa.application.port.in.SetupTotpUseCase;
import com.synapsys.api.mfa.application.dto.ConfirmTotpCommand;
import com.synapsys.api.mfa.application.dto.DisableTotpCommand;
import com.synapsys.api.mfa.application.dto.SetupTotpCommand;
import com.synapsys.api.mfa.domain.model.TotpSetupResult;
import com.synapsys.api.mfa.infrastructure.web.dto.TotpCodeRequest;
import com.synapsys.api.mfa.infrastructure.web.dto.TotpSetupResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa")
public class TotpController {

    private final SetupTotpUseCase setupUseCase;
    private final ConfirmTotpUseCase confirmUseCase;
    private final DisableTotpUseCase disableUseCase;

    public TotpController(SetupTotpUseCase setupUseCase,
                          ConfirmTotpUseCase confirmUseCase,
                          DisableTotpUseCase disableUseCase) {
        this.setupUseCase = setupUseCase;
        this.confirmUseCase = confirmUseCase;
        this.disableUseCase = disableUseCase;
    }

    @PostMapping("/setup")
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpSetupResponse> setup(@AuthenticationPrincipal CustomUserDetails caller) {
        TotpSetupResult result = setupUseCase.setup(new SetupTotpCommand(caller.getUserId(), caller.getEmail()));
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