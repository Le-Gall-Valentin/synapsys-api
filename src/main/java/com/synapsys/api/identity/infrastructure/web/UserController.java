package com.synapsys.api.identity.infrastructure.web;

import com.synapsys.api.authentication.infrastructure.security.CustomUserDetails;
import com.synapsys.api.identity.application.port.in.AdminResetTotpUseCase;
import com.synapsys.api.identity.application.port.in.DeactivateUserUseCase;
import com.synapsys.api.identity.application.port.in.GetCurrentUserUseCase;
import com.synapsys.api.identity.application.port.in.RegisterUseCase;
import com.synapsys.api.identity.domain.model.AdminResetTotpCommand;
import com.synapsys.api.identity.domain.model.DeactivateUserCommand;
import com.synapsys.api.identity.domain.model.RegisterCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.infrastructure.web.dto.RegisterRequest;
import com.synapsys.api.identity.infrastructure.web.dto.UserInfoResponse;
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
    private final AdminResetTotpUseCase adminResetTotpUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase,
                          RegisterUseCase registerUseCase,
                          DeactivateUserUseCase deactivateUserUseCase,
                          AdminResetTotpUseCase adminResetTotpUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.registerUseCase = registerUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
        this.adminResetTotpUseCase = adminResetTotpUseCase;
    }

    @GetMapping("/me")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal CustomUserDetails caller) {
        User user = getCurrentUserUseCase.getCurrentUser(caller.getUserId());
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.role()));
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
        return ResponseEntity.created(location).body(new UserInfoResponse(user.id(), user.username(), user.role()));
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
        adminResetTotpUseCase.reset(new AdminResetTotpCommand(id, caller.getUserId(), caller.getRole()));
        return ResponseEntity.noContent().build();
    }
}