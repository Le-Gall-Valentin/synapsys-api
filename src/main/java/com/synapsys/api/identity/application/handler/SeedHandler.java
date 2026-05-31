package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.application.port.in.SeedUseCase;
import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.port.out.CredentialSetupPort;
import com.synapsys.api.identity.domain.port.out.TotpRecordInitPort;
import com.synapsys.api.identity.domain.port.out.UserAdminPort;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationService
public class SeedHandler implements SeedUseCase {

    private static final Logger log = LoggerFactory.getLogger(SeedHandler.class);

    private final UserAdminPort userAdminPort;
    private final UserCommandPort userCommandPort;
    private final CredentialSetupPort credentialSetupPort;
    private final TotpRecordInitPort totpRecordInitPort;

    public SeedHandler(UserAdminPort userAdminPort,
                       UserCommandPort userCommandPort,
                       CredentialSetupPort credentialSetupPort,
                       TotpRecordInitPort totpRecordInitPort) {
        this.userAdminPort = userAdminPort;
        this.userCommandPort = userCommandPort;
        this.credentialSetupPort = credentialSetupPort;
        this.totpRecordInitPort = totpRecordInitPort;
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
        try {
            java.util.UUID userId = userCommandPort.createProfile(
                new CreateUserProfileCommand(username, email, Role.SUPER_ADMIN));
            credentialSetupPort.setup(userId, password);
            totpRecordInitPort.initForUser(userId);
            log.info("Default SUPER_ADMIN '{}' created", username);
        } catch (IdentityException.UsernameAlreadyExists | IdentityException.EmailAlreadyExists e) {
            log.info("SUPER_ADMIN already exists (concurrent startup), skipping");
        }
    }
}