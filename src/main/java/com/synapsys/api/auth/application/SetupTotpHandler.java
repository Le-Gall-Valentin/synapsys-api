package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.SetupTotpUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

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

        String candidate = secretGenerator.generateSecret();
        boolean saved = userTotpPort.saveTotpSecretIfAbsent(command.userId(), candidate);

        if (!saved) {
            // A concurrent request already saved a secret. Reload and return
            // the winning secret so both callers converge on the same QR code.
            User refreshed = userRepository.findById(command.userId())
                .orElseThrow(AuthException.UserNotFound::new);
            String existing = refreshed.totpSecret();
            if (existing == null) throw new AuthException.TotpNotEnabled();
            return new TotpSetupResult(existing, secretGenerator.buildOtpauthUri(existing, refreshed.email()));
        }

        return new TotpSetupResult(candidate, secretGenerator.buildOtpauthUri(candidate, user.email()));
    }
}