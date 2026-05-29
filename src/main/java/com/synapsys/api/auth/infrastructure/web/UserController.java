package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.*;
import com.synapsys.api.auth.infrastructure.security.CustomUserDetails;
import com.synapsys.api.auth.infrastructure.web.dto.RegisterRequest;
import com.synapsys.api.auth.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final RegisterUseCase registerUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final ResetUserTotpUseCase resetUserTotpUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase,
                          RegisterUseCase registerUseCase,
                          DeactivateUserUseCase deactivateUserUseCase,
                          ResetUserTotpUseCase resetUserTotpUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.registerUseCase = registerUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
        this.resetUserTotpUseCase = resetUserTotpUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal CustomUserDetails caller) {
        User user = getCurrentUserUseCase.getCurrentUser(caller.getUserId());
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.role(), user.totpEnabled()));
    }

    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     @AuthenticationPrincipal CustomUserDetails caller) {
        User user = registerUseCase.register(
            new RegisterCommand(request.username(), request.email(), request.password(), request.role()),
            caller.getRole()
        );
        URI location = URI.create("/api/users/" + user.id());
        return ResponseEntity.created(location).body(new UserInfoResponse(user.id(), user.username(), user.role(), user.totpEnabled()));
    }

    @DeleteMapping("/{id}")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                           @AuthenticationPrincipal CustomUserDetails caller) {
        deactivateUserUseCase.deactivate(new DeactivateUserCommand(id, caller.getUserId(), caller.getRole()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/2fa/reset")
    @RateLimiting(max = 10)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> resetTotp(@PathVariable UUID id,
                                          @AuthenticationPrincipal CustomUserDetails caller) {
        resetUserTotpUseCase.reset(new ResetUserTotpCommand(id, caller.getUserId(), caller.getRole()));
        return ResponseEntity.noContent().build();
    }
}