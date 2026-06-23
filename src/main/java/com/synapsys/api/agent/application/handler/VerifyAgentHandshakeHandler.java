package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.VerifyAgentHandshakeUseCase;
import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.VerifyHandshakeCommand;
import com.synapsys.api.agent.domain.port.out.AgentChallengeStorePort;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.agent.domain.port.out.AgentSignatureVerifierPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@ApplicationService
public class VerifyAgentHandshakeHandler implements VerifyAgentHandshakeUseCase {

    private final AgentChallengeStorePort challengeStore;
    private final AgentRepository agentRepository;
    private final AgentSignatureVerifierPort signatureVerifier;
    private final AgentPresencePort presence;

    public VerifyAgentHandshakeHandler(AgentChallengeStorePort challengeStore,
                                       AgentRepository agentRepository,
                                       AgentSignatureVerifierPort signatureVerifier,
                                       AgentPresencePort presence) {
        this.challengeStore = challengeStore;
        this.agentRepository = agentRepository;
        this.signatureVerifier = signatureVerifier;
        this.presence = presence;
    }

    @Override
    @Transactional
    public void verify(VerifyHandshakeCommand command, String ip, String nodeId) {
        Instant now = Instant.now();
        // Single-use: consume up front so even a failed attempt cannot be replayed.
        String nonce = challengeStore.consumeChallenge(command.connectionId())
            .orElseThrow(AgentException.HandshakeFailed::new);
        Agent agent = agentRepository.findById(command.agentId())
            .orElseThrow(AgentException.HandshakeFailed::new);
        agent.ensureConnectable();
        byte[] message = nonce.getBytes(StandardCharsets.UTF_8);
        if (!signatureVerifier.verify(agent.publicKey(), message, command.signature())) {
            throw new AgentException.HandshakeFailed();
        }
        agentRepository.markConnected(agent.id(), now, ip);
        presence.markPresent(agent.id(), nodeId, ip, now);
    }
}
