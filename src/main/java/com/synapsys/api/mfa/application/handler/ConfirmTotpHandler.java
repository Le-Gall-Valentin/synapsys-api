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
    private final UserTotpPort userTotpPort;
    private final TotpCodeReplayPort codeReplay;

    public ConfirmTotpHandler(UserTotpQueryPort userTotpQuery,
                              TotpCodeValidatorPort codeValidator,
                              UserTotpPort userTotpPort,
                              TotpCodeReplayPort codeReplay) {
        this.userTotpQuery = userTotpQuery;
        this.codeValidator = codeValidator;
        this.userTotpPort = userTotpPort;
        this.codeReplay = codeReplay;
    }

    @Override
    @Transactional
    public void confirm(ConfirmTotpCommand command) {
        UserTotpProfile user = userTotpQuery.findById(command.userId())
            .orElseThrow(MfaException.UserNotFound::new);

        if (user.totpSecret() == null) throw new MfaException.TotpSetupNotStarted();
        if (!codeValidator.isValid(user.totpSecret(), command.code())) throw new MfaException.TotpCodeInvalid();
        if (!codeReplay.markCodeUsedIfAbsent(command.userId(), command.code())) throw new MfaException.TotpCodeInvalid();

        userTotpPort.enableTotp(command.userId());
    }
}