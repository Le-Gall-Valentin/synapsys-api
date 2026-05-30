package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.dto.ResetUserTotpCommand;
import com.synapsys.api.mfa.domain.model.MfaException;
import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpPort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetUserTotpHandlerTest {

    @Mock UserTotpQueryPort userTotpQuery;
    @Mock UserTotpPort userTotpPort;

    private ResetUserTotpHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ResetUserTotpHandler(userTotpQuery, userTotpPort);
    }

    @Test
    void reset_totpEnabled_disablesTotp() {
        UserTotpProfile target = new UserTotpProfile(targetId, Role.USER, true, "SECRET");
        when(userTotpQuery.findById(targetId)).thenReturn(Optional.of(target));

        assertThatCode(() -> handler.reset(new ResetUserTotpCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .doesNotThrowAnyException();

        verify(userTotpPort).disableTotp(targetId);
    }

    @Test
    void reset_totpDisabled_noOp() {
        UserTotpProfile target = new UserTotpProfile(targetId, Role.USER, false, null);
        when(userTotpQuery.findById(targetId)).thenReturn(Optional.of(target));

        assertThatCode(() -> handler.reset(new ResetUserTotpCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .doesNotThrowAnyException();

        verify(userTotpPort, never()).disableTotp(any());
    }

    @Test
    void reset_selfReset_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.reset(new ResetUserTotpCommand(callerId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(MfaException.InsufficientPermissions.class);
    }

    @Test
    void reset_insufficientHierarchy_throwsInsufficientPermissions() {
        UserTotpProfile target = new UserTotpProfile(targetId, Role.SUPER_ADMIN, true, "SECRET");
        when(userTotpQuery.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> handler.reset(new ResetUserTotpCommand(targetId, callerId, Role.USER)))
            .isInstanceOf(MfaException.InsufficientPermissions.class);
    }

    @Test
    void reset_userNotFound_throwsUserNotFound() {
        when(userTotpQuery.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.reset(new ResetUserTotpCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(MfaException.UserNotFound.class);
    }
}