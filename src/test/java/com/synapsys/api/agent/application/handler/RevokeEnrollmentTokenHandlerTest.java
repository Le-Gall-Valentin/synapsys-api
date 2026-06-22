package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.RevokeEnrollmentTokenCommand;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeEnrollmentTokenHandlerTest {

    @Mock EnrollmentTokenRepository repository;

    private final UUID tokenId = UUID.randomUUID();
    private final UUID caller = UUID.randomUUID();

    private EnrollmentToken active() {
        return new EnrollmentToken(tokenId, "a", null, null, null,
            Instant.now().plus(1, ChronoUnit.HOURS), Instant.now(), caller);
    }

    @Test
    void revoke_notFound_throwsTokenNotFound() {
        when(repository.findById(tokenId)).thenReturn(Optional.empty());
        var handler = new RevokeEnrollmentTokenHandler(repository);
        assertThatThrownBy(() -> handler.revoke(new RevokeEnrollmentTokenCommand(tokenId, caller)))
            .isInstanceOf(AgentException.TokenNotFound.class);
        verify(repository, never()).markRevoked(any(), any(), any());
    }

    @Test
    void revoke_consumedToken_throwsTokenNotRevocable() {
        EnrollmentToken consumed = new EnrollmentToken(tokenId, "a", Instant.now(), null, null,
            Instant.now().plus(1, ChronoUnit.HOURS), Instant.now(), caller);
        when(repository.findById(tokenId)).thenReturn(Optional.of(consumed));
        var handler = new RevokeEnrollmentTokenHandler(repository);
        assertThatThrownBy(() -> handler.revoke(new RevokeEnrollmentTokenCommand(tokenId, caller)))
            .isInstanceOf(AgentException.TokenNotRevocable.class);
        verify(repository, never()).markRevoked(any(), any(), any());
    }

    @Test
    void revoke_active_callsMarkRevoked() {
        when(repository.findById(tokenId)).thenReturn(Optional.of(active()));
        when(repository.markRevoked(eq(tokenId), eq(caller), any())).thenReturn(true);
        var handler = new RevokeEnrollmentTokenHandler(repository);
        assertThatCode(() -> handler.revoke(new RevokeEnrollmentTokenCommand(tokenId, caller)))
            .doesNotThrowAnyException();
        verify(repository).markRevoked(eq(tokenId), eq(caller), any());
    }

    @Test
    void revoke_lostRace_throwsTokenNotRevocable() {
        when(repository.findById(tokenId)).thenReturn(Optional.of(active()));
        when(repository.markRevoked(eq(tokenId), eq(caller), any())).thenReturn(false);
        var handler = new RevokeEnrollmentTokenHandler(repository);
        assertThatThrownBy(() -> handler.revoke(new RevokeEnrollmentTokenCommand(tokenId, caller)))
            .isInstanceOf(AgentException.TokenNotRevocable.class);
    }
}
