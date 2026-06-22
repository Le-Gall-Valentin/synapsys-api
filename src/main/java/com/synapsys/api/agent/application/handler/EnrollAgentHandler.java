package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.dto.EnrollmentResult;
import com.synapsys.api.agent.application.port.in.EnrollAgentUseCase;
import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.Ed25519PublicKey;
import com.synapsys.api.agent.domain.model.EnrollAgentCommand;
import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.EnrollmentTokenStatus;
import com.synapsys.api.agent.domain.model.NewAgent;
import com.synapsys.api.agent.domain.port.out.AgentFingerprintPort;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.agent.domain.port.out.AgentTokenHashPort;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@ApplicationService
public class EnrollAgentHandler implements EnrollAgentUseCase {

    private final EnrollmentTokenRepository tokenRepository;
    private final AgentTokenHashPort hashPort;
    private final AgentRepository agentRepository;
    private final AgentFingerprintPort fingerprintPort;

    public EnrollAgentHandler(EnrollmentTokenRepository tokenRepository,
                              AgentTokenHashPort hashPort,
                              AgentRepository agentRepository,
                              AgentFingerprintPort fingerprintPort) {
        this.tokenRepository = tokenRepository;
        this.hashPort = hashPort;
        this.agentRepository = agentRepository;
        this.fingerprintPort = fingerprintPort;
    }

    @Override
    @Transactional
    public EnrollmentResult enroll(EnrollAgentCommand command) {
        Instant now = Instant.now();
        // Format check (400) before any token lookup; does not reveal token existence.
        Ed25519PublicKey publicKey = new Ed25519PublicKey(command.publicKey());

        // Uniform rejection for every token problem (anti-enumeration).
        EnrollmentToken token = tokenRepository.findByTokenHash(hashPort.hash(command.rawToken()))
            .orElseThrow(AgentException.EnrollmentRejected::new);
        if (token.deriveStatus(now) != EnrollmentTokenStatus.ACTIVE) {
            throw new AgentException.EnrollmentRejected();
        }
        if (agentRepository.existsByPublicKey(publicKey.value())) {
            throw new AgentException.PublicKeyAlreadyRegistered();
        }
        if (!tokenRepository.markConsumed(token.id(), now)) {
            throw new AgentException.EnrollmentRejected();
        }
        String fingerprint = fingerprintPort.fingerprint(publicKey.value());
        Agent agent = agentRepository.insert(new NewAgent(
            token.serverName(), publicKey.value(), fingerprint, token.id(), token.createdBy()));
        return new EnrollmentResult(agent.id(), agent.serverName(), agent.fingerprint());
    }
}
