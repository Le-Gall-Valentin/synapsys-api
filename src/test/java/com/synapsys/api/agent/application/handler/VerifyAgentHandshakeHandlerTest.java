package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.domain.model.VerifyHandshakeCommand;
import com.synapsys.api.agent.domain.port.out.AgentChallengeStorePort;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.agent.domain.port.out.AgentSignatureVerifierPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
class VerifyAgentHandshakeHandlerTest {

    @Mock AgentChallengeStorePort challengeStore;
    @Mock AgentRepository agentRepository;
    @Mock AgentSignatureVerifierPort signatureVerifier;
    @Mock AgentPresencePort presence;

    private VerifyAgentHandshakeHandler handler;
    private final UUID agentId = UUID.randomUUID();
    private final byte[] signature = new byte[64];

    @BeforeEach
    void setUp() {
        handler = new VerifyAgentHandshakeHandler(challengeStore, agentRepository, signatureVerifier, presence);
    }

    private Agent agent(AgentLifecycleStatus status) {
        return new Agent(agentId, "web-01", new byte[32], "fp", status, UUID.randomUUID(),
            Instant.now(), UUID.randomUUID(), null, null, null,
            status == AgentLifecycleStatus.REVOKED ? Instant.now() : null, null);
    }

    private VerifyHandshakeCommand cmd() {
        return new VerifyHandshakeCommand(agentId, "conn-1", signature);
    }

    @Test
    void verify_validSignature_marksConnectedAndPresent() {
        when(challengeStore.consumeChallenge("conn-1")).thenReturn(Optional.of("nonce"));
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent(AgentLifecycleStatus.ENROLLED)));
        when(signatureVerifier.verify(any(), any(), eq(signature))).thenReturn(true);
        when(agentRepository.markConnected(eq(agentId), any(), eq("1.2.3.4"))).thenReturn(true);

        assertThatCode(() -> handler.verify(cmd(), "1.2.3.4", "node-A")).doesNotThrowAnyException();

        verify(agentRepository).markConnected(eq(agentId), any(), eq("1.2.3.4"));
        verify(presence).markPresent(eq(agentId), eq("node-A"), eq("1.2.3.4"), any());
    }

    @Test
    void verify_agentRevokedMidConnect_throwsHandshakeFailed_andSkipsPresence() {
        when(challengeStore.consumeChallenge("conn-1")).thenReturn(Optional.of("nonce"));
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent(AgentLifecycleStatus.ENROLLED)));
        when(signatureVerifier.verify(any(), any(), eq(signature))).thenReturn(true);
        // Concurrent revoke between the read and the update: no row changes.
        when(agentRepository.markConnected(eq(agentId), any(), eq("1.2.3.4"))).thenReturn(false);

        assertThatThrownBy(() -> handler.verify(cmd(), "1.2.3.4", "node-A"))
            .isInstanceOf(AgentException.HandshakeFailed.class);
        verify(presence, never()).markPresent(any(), any(), any(), any());
    }

    @Test
    void verify_noChallenge_throwsHandshakeFailed() {
        when(challengeStore.consumeChallenge("conn-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.verify(cmd(), "1.2.3.4", "node-A"))
            .isInstanceOf(AgentException.HandshakeFailed.class);
        verify(agentRepository, never()).markConnected(any(), any(), any());
    }

    @Test
    void verify_agentNotFound_throwsHandshakeFailed() {
        when(challengeStore.consumeChallenge("conn-1")).thenReturn(Optional.of("nonce"));
        when(agentRepository.findById(agentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.verify(cmd(), "1.2.3.4", "node-A"))
            .isInstanceOf(AgentException.HandshakeFailed.class);
    }

    @Test
    void verify_revokedAgent_throwsHandshakeFailed() {
        when(challengeStore.consumeChallenge("conn-1")).thenReturn(Optional.of("nonce"));
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent(AgentLifecycleStatus.REVOKED)));
        assertThatThrownBy(() -> handler.verify(cmd(), "1.2.3.4", "node-A"))
            .isInstanceOf(AgentException.HandshakeFailed.class);
        verify(signatureVerifier, never()).verify(any(), any(), any());
    }

    @Test
    void verify_badSignature_throwsHandshakeFailed() {
        when(challengeStore.consumeChallenge("conn-1")).thenReturn(Optional.of("nonce"));
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent(AgentLifecycleStatus.ENROLLED)));
        when(signatureVerifier.verify(any(), any(), eq(signature))).thenReturn(false);
        assertThatThrownBy(() -> handler.verify(cmd(), "1.2.3.4", "node-A"))
            .isInstanceOf(AgentException.HandshakeFailed.class);
        verify(presence, never()).markPresent(any(), any(), any(), any());
    }
}
