package com.synapsys.api.authentication.application.handler;

import com.synapsys.api.authentication.application.port.in.ChangePasswordUseCase;
import com.synapsys.api.authentication.domain.model.AuthenticationException;
import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.domain.port.out.PasswordHasherPort;
import com.synapsys.api.authentication.domain.port.out.PasswordVerifierPort;
import com.synapsys.api.authentication.domain.port.out.UserCredentialPort;
import com.synapsys.api.authentication.domain.port.out.UserCredentialsPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ChangePasswordHandler implements ChangePasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordHandler.class);

    private final UserCredentialsPort userCredentialsPort;
    private final PasswordVerifierPort passwordVerifier;
    private final PasswordHasherPort passwordHasher;
    private final UserCredentialPort userCredentialPort;

    public ChangePasswordHandler(UserCredentialsPort userCredentialsPort,
                                 PasswordVerifierPort passwordVerifier,
                                 PasswordHasherPort passwordHasher,
                                 UserCredentialPort userCredentialPort) {
        this.userCredentialsPort = userCredentialsPort;
        this.passwordVerifier = passwordVerifier;
        this.passwordHasher = passwordHasher;
        this.userCredentialPort = userCredentialPort;
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        UserCredentials creds = userCredentialsPort.findById(userId)
            .orElseThrow(() -> {
                log.error("Data integrity: no credentials found for authenticated user {}", userId);
                return new AuthenticationException.DataIntegrityError();
            });
        if (!passwordVerifier.matches(currentPassword, creds.passwordHash())) {
            throw new AuthenticationException.InvalidCurrentPassword();
        }
        String newHash = passwordHasher.hash(newPassword);
        userCredentialPort.updatePasswordHash(userId, newHash);
    }
}