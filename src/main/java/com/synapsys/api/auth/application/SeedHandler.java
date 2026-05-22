package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.CreateUserCommand;
import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.port.in.SeedUseCase;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationService
public class SeedHandler implements SeedUseCase {

    private static final Logger log = LoggerFactory.getLogger(SeedHandler.class);

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;

    public SeedHandler(UserRepository userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void seedInitialSuperAdmin(String username, String email, String password) {
        if (!userRepository.isEmpty()) {
            log.info("Database already has users, skipping seed");
            return;
        }
        String hash = passwordHasher.hash(password);
        try {
            userRepository.save(new CreateUserCommand(username, email, hash, Role.SUPER_ADMIN));
            log.info("Default SUPER_ADMIN '{}' created", username);
        } catch (AuthException.UsernameAlreadyExists | AuthException.EmailAlreadyExists e) {
            log.info("SUPER_ADMIN already exists (concurrent startup), skipping");
        }
    }
}
