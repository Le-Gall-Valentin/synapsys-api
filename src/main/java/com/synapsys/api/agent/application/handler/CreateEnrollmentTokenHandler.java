package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.CreateEnrollmentTokenUseCase;
import com.synapsys.api.agent.domain.model.CreateEnrollmentTokenCommand;
import com.synapsys.api.agent.domain.model.IssuedToken;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenIssuerPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class CreateEnrollmentTokenHandler implements CreateEnrollmentTokenUseCase {

    private final EnrollmentTokenIssuerPort issuer;

    public CreateEnrollmentTokenHandler(EnrollmentTokenIssuerPort issuer) {
        this.issuer = issuer;
    }

    @Override
    @Transactional
    public IssuedToken create(CreateEnrollmentTokenCommand command) {
        return issuer.issue(command.serverName(), command.createdBy());
    }
}
