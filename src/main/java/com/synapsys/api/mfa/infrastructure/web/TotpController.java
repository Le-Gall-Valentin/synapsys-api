package com.synapsys.api.mfa.infrastructure.web;

import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import com.synapsys.api.shared.security.AuthenticatedUser;
import com.synapsys.api.shared.security.CurrentUser;
import com.synapsys.api.mfa.application.port.in.ConfirmTotpUseCase;
import com.synapsys.api.mfa.application.port.in.DisableTotpUseCase;
import com.synapsys.api.mfa.application.port.in.GetTotpStatusUseCase;
import com.synapsys.api.mfa.application.port.in.SetupTotpUseCase;
import com.synapsys.api.mfa.application.dto.ConfirmTotpCommand;
import com.synapsys.api.mfa.application.dto.DisableTotpCommand;
import com.synapsys.api.mfa.application.dto.SetupTotpCommand;
import com.synapsys.api.mfa.domain.model.TotpSetupResult;
import com.synapsys.api.mfa.infrastructure.web.dto.TotpCodeRequest;
import com.synapsys.api.mfa.infrastructure.web.dto.TotpSetupResponse;
import com.synapsys.api.mfa.infrastructure.web.dto.TotpStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa")
public class TotpController {

    private final SetupTotpUseCase setupUseCase;
    private final ConfirmTotpUseCase confirmUseCase;
    private final DisableTotpUseCase disableUseCase;
    private final GetTotpStatusUseCase statusUseCase;

    public TotpController(SetupTotpUseCase setupUseCase,
                          ConfirmTotpUseCase confirmUseCase,
                          DisableTotpUseCase disableUseCase,
                          GetTotpStatusUseCase statusUseCase) {
        this.setupUseCase = setupUseCase;
        this.confirmUseCase = confirmUseCase;
        this.disableUseCase = disableUseCase;
        this.statusUseCase = statusUseCase;
    }

    @PostMapping("/setup")
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpSetupResponse> setup(@CurrentUser AuthenticatedUser caller) {
        TotpSetupResult result = setupUseCase.setup(new SetupTotpCommand(caller.userId(), caller.email()));
        return ResponseEntity.ok(new TotpSetupResponse(result.otpauthUri(), result.secret()));
    }

    @PostMapping("/confirm")
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirm(@Valid @RequestBody TotpCodeRequest request,
                                        @CurrentUser AuthenticatedUser caller) {
        confirmUseCase.confirm(new ConfirmTotpCommand(caller.userId(), request.code()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disable(@Valid @RequestBody TotpCodeRequest request,
                                        @CurrentUser AuthenticatedUser caller) {
        disableUseCase.disable(new DisableTotpCommand(caller.userId(), request.code()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    @RateLimiting(max = 30)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpStatusResponse> status(@CurrentUser AuthenticatedUser caller) {
        boolean enabled = statusUseCase.isTotpEnabled(caller.userId());
        return ResponseEntity.ok(new TotpStatusResponse(enabled));
    }
}