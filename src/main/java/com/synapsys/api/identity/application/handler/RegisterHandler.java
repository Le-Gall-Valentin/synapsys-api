package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.application.port.in.RegisterUseCase;
import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.RegisterCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.CredentialSetupPort;
import com.synapsys.api.identity.domain.port.out.TotpRecordInitPort;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.shared.service.RoleHierarchy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class RegisterHandler implements RegisterUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterHandler.class);

    private final UserCommandPort userCommandPort;
    private final UserRepository userRepository;
    private final CredentialSetupPort credentialSetupPort;
    private final TotpRecordInitPort totpRecordInitPort;

    public RegisterHandler(UserCommandPort userCommandPort,
                           UserRepository userRepository,
                           CredentialSetupPort credentialSetupPort,
                           TotpRecordInitPort totpRecordInitPort) {
        this.userCommandPort = userCommandPort;
        this.userRepository = userRepository;
        this.credentialSetupPort = credentialSetupPort;
        this.totpRecordInitPort = totpRecordInitPort;
    }

    @Override
    @Transactional
    public User register(RegisterCommand command, Role callerRole) {
        if (!RoleHierarchy.canManage(callerRole, command.role())) {
            throw new IdentityException.InsufficientPermissions();
        }
        UUID userId = userCommandPort.createProfile(
            new CreateUserProfileCommand(command.username(), command.email(), command.role()));
        credentialSetupPort.setup(userId, command.rawPassword());
        totpRecordInitPort.initForUser(userId);
        log.info("User {} registered with role {}", userId, command.role());
        return userRepository.findById(userId).orElseThrow(IdentityException.UserNotFound::new);
    }
}