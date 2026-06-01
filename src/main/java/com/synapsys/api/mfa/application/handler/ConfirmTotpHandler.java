package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.port.in.ConfirmTotpUseCase;
import com.synapsys.api.mfa.domain.model.*;
import com.synapsys.api.mfa.application.dto.*;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ConfirmTotpHandler implements ConfirmTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final TotpCodeValidatorPort codeValidator;
    private final UserTotpLifecyclePort userTotpLifecyclePort;
    private final TotpCodeReplayPort codeReplay;

    public ConfirmTotpHandler(UserTotpQueryPort userTotpQuery,
                              TotpCodeValidatorPort codeValidator,
                              UserTotpLifecyclePort userTotpLifecyclePort,
                              TotpCodeReplayPort codeReplay) {
        this.userTotpQuery = userTotpQuery;
        this.codeValidator = codeValidator;
        this.userTotpLifecyclePort = userTotpLifecyclePort;
        this.codeReplay = codeReplay;
    }

    @Override
    @Transactional
    public void confirm(ConfirmTotpCommand command) {
        UserTotpProfile user = userTotpQuery.findById(command.userId())
            .orElseThrow(MfaException.UserNotFound::new);

        if (user.totpEnabled()) throw new MfaException.TotpAlreadyEnabled();
        String secret = user.totpSecret().orElseThrow(MfaException.TotpSetupNotStarted::new);
        if (!codeValidator.isValid(secret, command.code())) throw new MfaException.TotpCodeInvalid();
        if (!codeReplay.markCodeUsedIfAbsent(command.userId(), command.code())) throw new MfaException.TotpCodeInvalid();

        userTotpLifecyclePort.enableTotp(command.userId());
    }
}