package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.*;
import com.synapsys.api.auth.infrastructure.security.CustomUserDetails;
import com.synapsys.api.auth.infrastructure.web.dto.RegisterRequest;
import com.synapsys.api.auth.infrastructure.web.dto.UserInfoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final RegisterUseCase registerUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase,
                          RegisterUseCase registerUseCase,
                          DeactivateUserUseCase deactivateUserUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.registerUseCase = registerUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal CustomUserDetails caller) {
        User user = getCurrentUserUseCase.getCurrentUser(caller.getUserId());
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.role()));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     @AuthenticationPrincipal CustomUserDetails caller) {
        User user = registerUseCase.register(
            new RegisterCommand(request.username(), request.email(), request.password(), request.role()),
            caller.getRole()
        );
        return ResponseEntity.status(201).body(
            new UserInfoResponse(user.id(), user.username(), user.role())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                           @AuthenticationPrincipal CustomUserDetails caller) {
        deactivateUserUseCase.deactivate(id, caller.getUserId(), caller.getRole());
        return ResponseEntity.noContent().build();
    }
}