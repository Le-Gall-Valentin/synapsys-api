package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.ResetUserTotpUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.auth.domain.service.RoleHierarchy;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ResetUserTotpHandler implements ResetUserTotpUseCase {

    private final UserRepository userRepository;
    private final UserTotpPort userTotpPort;

    public ResetUserTotpHandler(UserRepository userRepository, UserTotpPort userTotpPort) {
        this.userRepository = userRepository;
        this.userTotpPort = userTotpPort;
    }

    @Override
    @Transactional
    public void reset(ResetUserTotpCommand command) {
        if (command.callerId().equals(command.targetUserId())) {
            throw new AuthException.InsufficientPermissions();
        }

        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (!RoleHierarchy.canManage(command.callerRole(), target.role())) {
            throw new AuthException.InsufficientPermissions();
        }

        if (!target.totpEnabled()) {
            return;
        }

        userTotpPort.disableTotp(command.targetUserId());
    }
}