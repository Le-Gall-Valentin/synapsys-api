package com.synapsys.api.auth.application.handler;

import com.synapsys.api.auth.application.dto.AdminResetTotpCommand;
import com.synapsys.api.auth.application.port.in.AdminResetTotpUseCase;
import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.out.MfaAdminResetTotpPort;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.service.RoleHierarchy;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class AdminResetTotpHandler implements AdminResetTotpUseCase {

    private final UserRepository userRepository;
    private final MfaAdminResetTotpPort mfaResetTotp;

    public AdminResetTotpHandler(UserRepository userRepository, MfaAdminResetTotpPort mfaResetTotp) {
        this.userRepository = userRepository;
        this.mfaResetTotp = mfaResetTotp;
    }

    @Override
    @Transactional
    public void reset(AdminResetTotpCommand command) {
        if (command.callerId().equals(command.targetUserId()))
            throw new AuthException.InsufficientPermissions();

        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(AuthException.UserNotFound::new);

        if (!RoleHierarchy.canManage(command.callerRole(), target.role()))
            throw new AuthException.InsufficientPermissions();

        mfaResetTotp.disableTotpIfEnabled(command.targetUserId());
    }
}