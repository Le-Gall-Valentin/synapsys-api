package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTotpStatusHandlerTest {

    @Mock UserTotpQueryPort userTotpQuery;

    private GetTotpStatusHandler handler;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetTotpStatusHandler(userTotpQuery);
    }

    @Test
    void isTotpEnabled_userWithTotpEnabled_returnsTrue() {
        UserTotpProfile profile = new UserTotpProfile(userId, Role.USER, true, "SECRET");
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThat(handler.isTotpEnabled(userId)).isTrue();
    }

    @Test
    void isTotpEnabled_userNotFound_returnsFalse() {
        when(userTotpQuery.findById(userId)).thenReturn(Optional.empty());

        assertThat(handler.isTotpEnabled(userId)).isFalse();
    }
}