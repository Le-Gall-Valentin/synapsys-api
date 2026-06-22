package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.dto.EnrollmentResult;
import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.domain.model.EnrollAgentCommand;
import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.NewAgent;
import com.synapsys.api.agent.domain.port.out.AgentFingerprintPort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.agent.domain.port.out.AgentTokenHashPort;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollAgentHandlerTest {

    @Mock EnrollmentTokenRepository tokenRepository;
    @Mock AgentTokenHashPort hashPort;
    @Mock AgentRepository agentRepository;
    @Mock AgentFingerprintPort fingerprintPort;

    private EnrollAgentHandler handler;
    private final UUID creator = UUID.randomUUID();
    private final UUID tokenId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new EnrollAgentHandler(tokenRepository, hashPort, agentRepository, fingerprintPort);
    }

    private EnrollmentToken activeToken() {
        return new EnrollmentToken(tokenId, "web-01", null, null, null,
            Instant.now().plus(1, ChronoUnit.HOURS), Instant.now(), creator);
    }

    private byte[] validKey() { return new byte[32]; }

    @Test
    void enroll_validTokenAndKey_createsPendingAgent() {
        when(hashPort.hash("raw")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(activeToken()));
        when(agentRepository.existsByPublicKey(any())).thenReturn(false);
        when(tokenRepository.markConsumed(eq(tokenId), any())).thenReturn(true);
        when(fingerprintPort.fingerprint(any())).thenReturn("fp");
        when(agentRepository.insert(any())).thenAnswer(inv -> {
            NewAgent n = inv.getArgument(0);
            return new Agent(UUID.randomUUID(), n.serverName(), n.publicKey(), n.fingerprint(),
                AgentLifecycleStatus.ENROLLED, n.enrollmentTokenId(), Instant.now(), n.enrolledBy(),
                null, null, null, null, null);
        });

        EnrollmentResult result = handler.enroll(new EnrollAgentCommand("raw", validKey()));

        assertThat(result.serverName()).isEqualTo("web-01");
        assertThat(result.fingerprint()).isEqualTo("fp");
        verify(tokenRepository).markConsumed(eq(tokenId), any());
        verify(agentRepository).insert(any());
    }

    @Test
    void enroll_unknownToken_throwsEnrollmentRejected() {
        when(hashPort.hash("raw")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.enroll(new EnrollAgentCommand("raw", validKey())))
            .isInstanceOf(AgentException.EnrollmentRejected.class);
        verify(agentRepository, never()).insert(any());
    }

    @Test
    void enroll_consumedToken_throwsEnrollmentRejected() {
        EnrollmentToken consumed = new EnrollmentToken(tokenId, "web-01", Instant.now(), null, null,
            Instant.now().plus(1, ChronoUnit.HOURS), Instant.now(), creator);
        when(hashPort.hash("raw")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(consumed));
        assertThatThrownBy(() -> handler.enroll(new EnrollAgentCommand("raw", validKey())))
            .isInstanceOf(AgentException.EnrollmentRejected.class);
    }

    @Test
    void enroll_badKeyLength_throwsInvalidPublicKey() {
        assertThatThrownBy(() -> handler.enroll(new EnrollAgentCommand("raw", new byte[31])))
            .isInstanceOf(AgentException.InvalidPublicKey.class);
        verify(tokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void enroll_duplicateKey_throwsPublicKeyAlreadyRegistered() {
        when(hashPort.hash("raw")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(activeToken()));
        when(agentRepository.existsByPublicKey(any())).thenReturn(true);
        assertThatThrownBy(() -> handler.enroll(new EnrollAgentCommand("raw", validKey())))
            .isInstanceOf(AgentException.PublicKeyAlreadyRegistered.class);
        verify(tokenRepository, never()).markConsumed(any(), any());
    }

    @Test
    void enroll_consumeRace_throwsEnrollmentRejected() {
        when(hashPort.hash("raw")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(activeToken()));
        when(agentRepository.existsByPublicKey(any())).thenReturn(false);
        when(tokenRepository.markConsumed(eq(tokenId), any())).thenReturn(false);
        assertThatThrownBy(() -> handler.enroll(new EnrollAgentCommand("raw", validKey())))
            .isInstanceOf(AgentException.EnrollmentRejected.class);
        verify(agentRepository, never()).insert(any());
    }
}
