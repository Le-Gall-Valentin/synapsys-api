package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetUserTotpHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserTotpPort userTotpPort;

    private ResetUserTotpHandler handler;

    private final UUID superAdminId = UUID.randomUUID();
    private final UUID adminId      = UUID.randomUUID();
    private final UUID userId       = UUID.randomUUID();

    private final User superAdmin = user(superAdminId, Role.SUPER_ADMIN, true);
    private final User admin      = user(adminId, Role.ADMIN, true);
    private final User regularUser = user(userId, Role.USER, true);

    @BeforeEach
    void setUp() {
        handler = new ResetUserTotpHandler(userRepository, userTotpPort);
    }

    @Test
    void reset_superAdmin_canResetUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));

        assertThatNoException().isThrownBy(() ->
            handler.reset(new ResetUserTotpCommand(userId, superAdminId, Role.SUPER_ADMIN)));

        verify(userTotpPort).disableTotp(userId);
    }

    @Test
    void reset_superAdmin_canResetAdmin() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatNoException().isThrownBy(() ->
            handler.reset(new ResetUserTotpCommand(adminId, superAdminId, Role.SUPER_ADMIN)));

        verify(userTotpPort).disableTotp(adminId);
    }

    @Test
    void reset_admin_canResetUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));

        assertThatNoException().isThrownBy(() ->
            handler.reset(new ResetUserTotpCommand(userId, adminId, Role.ADMIN)));

        verify(userTotpPort).disableTotp(userId);
    }

    @Test
    void reset_admin_cannotResetAnotherAdmin() {
        UUID otherAdminId = UUID.randomUUID();
        User otherAdmin = user(otherAdminId, Role.ADMIN, true);
        when(userRepository.findById(otherAdminId)).thenReturn(Optional.of(otherAdmin));

        assertThatThrownBy(() ->
            handler.reset(new ResetUserTotpCommand(otherAdminId, adminId, Role.ADMIN)))
            .isInstanceOf(AuthException.InsufficientPermissions.class);

        verifyNoInteractions(userTotpPort);
    }

    @Test
    void reset_cannotResetOwnTotp() {
        assertThatThrownBy(() ->
            handler.reset(new ResetUserTotpCommand(superAdminId, superAdminId, Role.SUPER_ADMIN)))
            .isInstanceOf(AuthException.InsufficientPermissions.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void reset_targetNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            handler.reset(new ResetUserTotpCommand(userId, superAdminId, Role.SUPER_ADMIN)))
            .isInstanceOf(AuthException.UserNotFound.class);
    }

    @Test
    void reset_idempotent_whenTotpNotEnabled() {
        User noTotp = user(userId, Role.USER, false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(noTotp));

        assertThatNoException().isThrownBy(() ->
            handler.reset(new ResetUserTotpCommand(userId, superAdminId, Role.SUPER_ADMIN)));

        verifyNoInteractions(userTotpPort);
    }

    private User user(UUID id, Role role, boolean totpEnabled) {
        return new User(id, "user-" + id, id + "@test.com", "hash",
            role, true, Instant.now(), totpEnabled ? "SECRET" : null, totpEnabled);
    }
}