package com.synapsys.api.identity.infrastructure.web;

import com.synapsys.api.identity.application.port.in.AdminResetTotpUseCase;
import com.synapsys.api.identity.application.port.in.ChangeMyPasswordUseCase;
import com.synapsys.api.shared.security.AuthenticatedUser;
import com.synapsys.api.shared.security.CurrentUser;
import com.synapsys.api.identity.application.port.in.DeactivateUserUseCase;
import com.synapsys.api.identity.application.port.in.GetCurrentUserUseCase;
import com.synapsys.api.identity.application.port.in.RegisterUseCase;
import com.synapsys.api.identity.application.port.in.UpdateMyProfileUseCase;
import com.synapsys.api.identity.domain.model.AdminResetTotpCommand;
import com.synapsys.api.identity.domain.model.ChangeMyPasswordCommand;
import com.synapsys.api.identity.domain.model.DeactivateUserCommand;
import com.synapsys.api.identity.domain.model.RegisterCommand;
import com.synapsys.api.identity.domain.model.UpdateProfileCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.model.UserSelfView;
import com.synapsys.api.identity.infrastructure.web.dto.ChangePasswordRequest;
import com.synapsys.api.identity.infrastructure.web.dto.RegisterRequest;
import com.synapsys.api.identity.infrastructure.web.dto.UpdateProfileRequest;
import com.synapsys.api.identity.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final ChangeMyPasswordUseCase changeMyPasswordUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase,
                          RegisterUseCase registerUseCase,
                          DeactivateUserUseCase deactivateUserUseCase,
                          AdminResetTotpUseCase adminResetTotpUseCase,
                          UpdateMyProfileUseCase updateMyProfileUseCase,
                          ChangeMyPasswordUseCase changeMyPasswordUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.registerUseCase = registerUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
        this.adminResetTotpUseCase = adminResetTotpUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.changeMyPasswordUseCase = changeMyPasswordUseCase;
    }

    @GetMapping("/me")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> me(@CurrentUser AuthenticatedUser caller) {
        UserSelfView view = getCurrentUserUseCase.getCurrentUser(caller.userId());
        return ResponseEntity.ok(new UserInfoResponse(
            view.id(), view.username(), view.email(), view.role(), view.createdAt(), view.totpEnabled()));
    }

    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     @CurrentUser AuthenticatedUser caller) {
        User user = registerUseCase.register(
            new RegisterCommand(request.username(), request.email(), request.password(), request.role()),
            caller.role()
        );
        URI location = URI.create("/api/users/" + user.id());
        return ResponseEntity.created(location).body(new UserInfoResponse(
            user.id(), user.username(), user.email(), user.role(), user.createdAt(), false));
    }

    @DeleteMapping("/{id}")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                           @CurrentUser AuthenticatedUser caller) {
        deactivateUserUseCase.deactivate(new DeactivateUserCommand(id, caller.userId(), caller.role()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/2fa/reset")
    @RateLimiting(max = 10)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> resetTotp(@PathVariable UUID id,
                                          @CurrentUser AuthenticatedUser caller) {
        adminResetTotpUseCase.reset(new AdminResetTotpCommand(id, caller.userId(), caller.role()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                              @CurrentUser AuthenticatedUser caller) {
        updateMyProfileUseCase.updateProfile(
            new UpdateProfileCommand(caller.userId(), request.username(), request.email()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @CurrentUser AuthenticatedUser caller) {
        changeMyPasswordUseCase.changeMyPassword(
            new ChangeMyPasswordCommand(caller.userId(), request.currentPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }
}