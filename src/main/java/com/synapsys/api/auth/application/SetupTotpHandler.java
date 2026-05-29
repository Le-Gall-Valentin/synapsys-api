package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.SetupTotpUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class SetupTotpHandler implements SetupTotpUseCase {

    private final UserRepository userRepository;
    private final TotpSecretGeneratorPort secretGenerator;
    private final UserTotpPort userTotpPort;

    public SetupTotpHandler(UserRepository userRepository,
                            TotpSecretGeneratorPort secretGenerator,
                            UserTotpPort userTotpPort) {
        this.userRepository = userRepository;
        this.secretGenerator = secretGenerator;
        this.userTotpPort = userTotpPort;
    }

    @Override
    @Transactional
    public TotpSetupResult setup(SetupTotpCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (user.totpEnabled()) {
            throw new AuthException.TotpAlreadyEnabled();
        }

        String secret = secretGenerator.generateSecret();
        userTotpPort.saveTotpSecret(command.userId(), secret);
        String uri = secretGenerator.buildOtpauthUri(secret, user.email());
        return new TotpSetupResult(secret, uri);
    }
}