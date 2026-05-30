package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.dto.ConfirmTotpCommand;
import com.synapsys.api.mfa.domain.model.MfaException;
import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.synapsys.api.mfa.domain.port.out.TotpCodeValidatorPort;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmTotpHandlerTest {

    @Mock UserTotpQueryPort userTotpQuery;
    @Mock TotpCodeValidatorPort codeValidator;
    @Mock UserTotpPort userTotpPort;
    @Mock TotpCodeReplayPort codeReplay;

    private ConfirmTotpHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final String secret = "MYSECRET";

    @BeforeEach
    void setUp() {
        handler = new ConfirmTotpHandler(userTotpQuery, codeValidator, userTotpPort, codeReplay);
    }

    @Test
    void confirm_validCode_enablesTotp() {
        UserTotpProfile profile = new UserTotpProfile(userId, Role.USER, false, secret);
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);

        assertThatCode(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .doesNotThrowAnyException();

        verify(userTotpPort).enableTotp(userId);
    }

    @Test
    void confirm_secretNull_throwsTotpSetupNotStarted() {
        UserTotpProfile profile = new UserTotpProfile(userId, Role.USER, false, null);
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpSetupNotStarted.class);
    }

    @Test
    void confirm_wrongCode_throwsTotpCodeInvalid() {
        UserTotpProfile profile = new UserTotpProfile(userId, Role.USER, false, secret);
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "000000")).thenReturn(false);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "000000")))
            .isInstanceOf(MfaException.TotpCodeInvalid.class);
    }

    @Test
    void confirm_replayedCode_throwsTotpCodeInvalid() {
        UserTotpProfile profile = new UserTotpProfile(userId, Role.USER, false, secret);
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpCodeInvalid.class);
    }
}