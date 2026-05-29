package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.ConfirmTotpUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ConfirmTotpHandler implements ConfirmTotpUseCase {

    private final UserRepository userRepository;
    private final TotpCodeValidatorPort codeValidator;
    private final UserTotpPort userTotpPort;

    public ConfirmTotpHandler(UserRepository userRepository,
                              TotpCodeValidatorPort codeValidator,
                              UserTotpPort userTotpPort) {
        this.userRepository = userRepository;
        this.codeValidator = codeValidator;
        this.userTotpPort = userTotpPort;
    }

    @Override
    @Transactional
    public void confirm(ConfirmTotpCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (user.totpSecret() == null) {
            throw new AuthException.TotpNotEnabled();
        }

        if (!codeValidator.isValid(user.totpSecret(), command.code())) {
            throw new AuthException.TotpCodeInvalid();
        }

        userTotpPort.enableTotp(command.userId());
    }
}