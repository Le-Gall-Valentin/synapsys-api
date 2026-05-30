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
    private final TotpChallengeStorePort challengeStore;

    public DisableTotpHandler(UserRepository userRepository,
                              UserTotpPort userTotpPort,
                              TotpCodeValidatorPort codeValidator,
                              TotpChallengeStorePort challengeStore) {
        this.userRepository = userRepository;
        this.userTotpPort = userTotpPort;
        this.codeValidator = codeValidator;
        this.challengeStore = challengeStore;
    }

    @Override
    @Transactional
    public void disable(DisableTotpCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (!user.totpEnabled()) {
            throw new AuthException.TotpNotEnabled();
        }

        if (user.totpSecret() == null) {
            throw new AuthException.TotpCodeInvalid();
        }

        if (!codeValidator.isValid(user.totpSecret(), command.code())) {
            throw new AuthException.TotpCodeInvalid();
        }

        // Consume the code atomically — prevents replay within the 90-second validity window.
        // Same invariant as VerifyTotpChallengeHandler and ConfirmTotpHandler.
        if (!challengeStore.markCodeUsedIfAbsent(command.userId(), command.code())) {
            throw new AuthException.TotpCodeInvalid();
        }

        userTotpPort.disableTotp(command.userId());
    }
}