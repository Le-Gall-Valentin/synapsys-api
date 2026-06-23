package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.RevokeEnrollmentTokenUseCase;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.RevokeEnrollmentTokenCommand;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@ApplicationService
public class RevokeEnrollmentTokenHandler implements RevokeEnrollmentTokenUseCase {

    private final EnrollmentTokenRepository repository;

    public RevokeEnrollmentTokenHandler(EnrollmentTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void revoke(RevokeEnrollmentTokenCommand command) {
        Instant now = Instant.now();
        EnrollmentToken token = repository.findById(command.tokenId())
            .orElseThrow(AgentException.TokenNotFound::new);
        token.ensureRevocable(now);
        if (!repository.markRevoked(command.tokenId(), command.callerId(), now)) {
            throw new AgentException.TokenNotRevocable();
        }
    }
}
