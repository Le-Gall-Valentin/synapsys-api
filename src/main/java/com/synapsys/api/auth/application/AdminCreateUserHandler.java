package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.AdminCreateUserUseCase;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationService
public class AdminCreateUserHandler implements AdminCreateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdminCreateUserHandler.class);

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;

    public AdminCreateUserHandler(UserRepository userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User create(AdminCreateUserCommand command) {
        if (!RoleCreationPolicy.canCreate(command.callerRole(), command.targetRole())) {
            throw new AuthException.InsufficientPermissions(command.callerRole(), command.targetRole());
        }
        if (userRepository.findByUsername(command.username()).isPresent()) {
            throw new AuthException.UsernameAlreadyExists();
        }
        String passwordHash = passwordHasher.hash(command.password());
        User created = userRepository.save(new CreateUserCommand(
            command.username(), command.email(), passwordHash, command.targetRole()
        ));
        log.info("User {} created by {} with role {}", created.id(), command.callerRole(), created.role());
        return created;
    }
}