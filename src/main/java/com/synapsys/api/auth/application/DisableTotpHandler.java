package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.DisableTotpUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class DisableTotpHandler implements DisableTotpUseCase {

    private final UserRepository userRepository;
    private final UserTotpPort userTotpPort;
    private final TotpCodeValidatorPort codeValidator;

    public DisableTotpHandler(UserRepository userRepository,
                              UserTotpPort userTotpPort,
                              TotpCodeValidatorPort codeValidator) {
        this.userRepository = userRepository;
        this.userTotpPort = userTotpPort;
        this.codeValidator = codeValidator;
    }

    @Override
    @Transactional
    public void disable(DisableTotpCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (!user.totpEnabled()) {
            throw new AuthException.TotpNotEnabled();
        }

        if (!codeValidator.isValid(user.totpSecret(), command.code())) {
            throw new AuthException.TotpCodeInvalid();
        }

        userTotpPort.disableTotp(command.userId());
    }
}
