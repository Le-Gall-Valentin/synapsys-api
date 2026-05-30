package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.CreateUserCommand;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.auth.domain.port.in.SeedUseCase;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import com.synapsys.api.auth.domain.port.out.UserAdminPort;
import com.synapsys.api.auth.domain.port.out.UserCommandPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationService
public class SeedHandler implements SeedUseCase {

    private static final Logger log = LoggerFactory.getLogger(SeedHandler.class);

    private final UserAdminPort userAdminPort;
    private final UserCommandPort userCommandPort;
    private final PasswordHasherPort passwordHasher;

    public SeedHandler(UserAdminPort userAdminPort, UserCommandPort userCommandPort, PasswordHasherPort passwordHasher) {
        this.userAdminPort = userAdminPort;
        this.userCommandPort = userCommandPort;
        this.passwordHasher = passwordHasher;
    }

    // Intentionally non-transactional: the catch handles concurrent startup races where two
    // instances call isEmpty() before either saves. The adapter surfaces the constraint violation
    // immediately so it is catchable before any outer transaction commits.
    @Override
    public void seedInitialSuperAdmin(String username, String email, String password) {
        if (!userAdminPort.isEmpty()) {
            log.info("Database already has users, skipping seed");
            return;
        }
        String hash = passwordHasher.hash(password);
        try {
            userCommandPort.save(new CreateUserCommand(username, email, hash, Role.SUPER_ADMIN));
            log.info("Default SUPER_ADMIN '{}' created", username);
        } catch (AuthException.UsernameAlreadyExists | AuthException.EmailAlreadyExists e) {
            log.info("SUPER_ADMIN already exists (concurrent startup), skipping");
        }
    }
}