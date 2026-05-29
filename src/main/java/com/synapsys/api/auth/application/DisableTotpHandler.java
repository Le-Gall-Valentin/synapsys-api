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

    public DisableTotpHandler(UserRepository userRepository, UserTotpPort userTotpPort) {
        this.userRepository = userRepository;
        this.userTotpPort = userTotpPort;
    }

    @Override
    @Transactional
    public void disable(DisableTotpCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (!user.totpEnabled()) {
            throw new AuthException.TotpNotEnabled();
        }

        userTotpPort.disableTotp(command.userId());
    }
}