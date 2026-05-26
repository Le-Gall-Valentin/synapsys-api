package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.service.RoleHierarchy;
import com.synapsys.api.auth.domain.port.in.RegisterUseCase;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;


@ApplicationService
public class RegisterHandler implements RegisterUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterHandler.class);

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;

    public RegisterHandler(UserRepository userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public User register(RegisterCommand command, Role callerRole) {
        if (!RoleHierarchy.canManage(callerRole, command.role())) {
            throw new AuthException.InsufficientPermissions();
        }
        String passwordHash = passwordHasher.hash(command.password());
        User created = userRepository.save(new CreateUserCommand(
            command.username(), command.email(), passwordHash, command.role()
        ));
        log.info("User {} registered with role {} by caller {}", created.id(), created.role(), callerRole);
        return created;
    }
}