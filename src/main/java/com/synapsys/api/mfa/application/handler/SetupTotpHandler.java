package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.port.in.SetupTotpUseCase;
import com.synapsys.api.mfa.domain.model.*;
import com.synapsys.api.mfa.application.dto.*;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class SetupTotpHandler implements SetupTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final TotpSecretGeneratorPort secretGenerator;
    private final UserTotpPort userTotpPort;

    public SetupTotpHandler(UserTotpQueryPort userTotpQuery,
                            TotpSecretGeneratorPort secretGenerator,
                            UserTotpPort userTotpPort) {
        this.userTotpQuery = userTotpQuery;
        this.secretGenerator = secretGenerator;
        this.userTotpPort = userTotpPort;
    }

    @Override
    @Transactional
    public TotpSetupResult setup(SetupTotpCommand command) {
        UserTotpProfile user = userTotpQuery.findById(command.userId())
            .orElseThrow(MfaException.UserNotFound::new);

        if (user.totpEnabled()) throw new MfaException.TotpAlreadyEnabled();

        String candidate = secretGenerator.generateSecret();
        boolean saved = userTotpPort.saveTotpSecretIfAbsent(command.userId(), candidate);

        if (!saved) {
            UserTotpProfile refreshed = userTotpQuery.findById(command.userId())
                .orElseThrow(MfaException.UserNotFound::new);
            String existing = refreshed.totpSecret();
            if (existing == null) throw new MfaException.TotpSetupNotStarted();
            return new TotpSetupResult(existing, secretGenerator.buildOtpauthUri(existing, command.email()));
        }

        return new TotpSetupResult(candidate, secretGenerator.buildOtpauthUri(candidate, command.email()));
    }
}