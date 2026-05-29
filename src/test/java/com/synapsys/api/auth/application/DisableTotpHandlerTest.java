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
class DisableTotpHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserTotpPort userTotpPort;

    private DisableTotpHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final User enabledUser = new User(
        userId, "user1", "user1@test.com", "hash",
        Role.USER, true, Instant.now(), "SECRETBASE32XXXX", true
    );

    @BeforeEach
    void setUp() {
        handler = new DisableTotpHandler(userRepository, userTotpPort);
    }

    @Test
    void disable_totpEnabled_disablesIt() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(enabledUser));

        assertThatNoException().isThrownBy(() -> handler.disable(new DisableTotpCommand(userId)));

        verify(userTotpPort).disableTotp(userId);
    }

    @Test
    void disable_totpNotEnabled_throws() {
        User notEnabled = new User(userId, "user1", "user1@test.com", "hash",
            Role.USER, true, Instant.now(), null, false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(notEnabled));

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId)))
            .isInstanceOf(AuthException.TotpNotEnabled.class);

        verifyNoInteractions(userTotpPort);
    }

    @Test
    void disable_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId)))
            .isInstanceOf(AuthException.UserNotFound.class);
    }
}