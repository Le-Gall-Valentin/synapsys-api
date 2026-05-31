package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.synapsys.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotpCodeVerificationServiceTest {

    @Mock UserTotpQueryPort userTotpQuery;
    @Mock TotpCodeValidatorPort codeValidator;
    @Mock TotpCodeReplayPort codeReplay;

    private TotpCodeVerificationService service;

    private final UUID userId = UUID.randomUUID();
    private final String secret = "MYSECRET";

    @BeforeEach
    void setUp() {
        service = new TotpCodeVerificationService(userTotpQuery, codeValidator, codeReplay);
    }

    @Test
    void verifyAndConsume_validCode_returnsTrue() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, secret);
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);

        assertThat(service.verifyAndConsume(userId, "123456")).isTrue();
    }

    @Test
    void verifyAndConsume_userNotFound_returnsFalse() {
        when(userTotpQuery.findById(userId)).thenReturn(Optional.empty());

        assertThat(service.verifyAndConsume(userId, "123456")).isFalse();
    }

    @Test
    void verifyAndConsume_secretNull_returnsFalse() {
        UserTotpProfile profile = new UserTotpProfile(userId,false, null);
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThat(service.verifyAndConsume(userId, "123456")).isFalse();
    }

    @Test
    void verifyAndConsume_invalidCode_returnsFalse() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, secret);
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "000000")).thenReturn(false);

        assertThat(service.verifyAndConsume(userId, "000000")).isFalse();
    }

    @Test
    void verifyAndConsume_replayedCode_returnsFalse() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, secret);
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThat(service.verifyAndConsume(userId, "123456")).isFalse();
    }
}