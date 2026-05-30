package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.port.in.ResetUserTotpUseCase;
import com.synapsys.api.mfa.domain.model.*;
import com.synapsys.api.mfa.application.dto.*;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.service.RoleHierarchy;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ResetUserTotpHandler implements ResetUserTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final UserTotpPort userTotpPort;

    public ResetUserTotpHandler(UserTotpQueryPort userTotpQuery, UserTotpPort userTotpPort) {
        this.userTotpQuery = userTotpQuery;
        this.userTotpPort = userTotpPort;
    }

    @Override
    @Transactional
    public void reset(ResetUserTotpCommand command) {
        if (command.callerId().equals(command.targetUserId()))
            throw new MfaException.InsufficientPermissions();

        UserTotpProfile target = userTotpQuery.findById(command.targetUserId())
            .orElseThrow(MfaException.UserNotFound::new);

        if (!RoleHierarchy.canManage(command.callerRole(), target.role()))
            throw new MfaException.InsufficientPermissions();

        if (!target.totpEnabled()) return;

        userTotpPort.disableTotp(command.targetUserId());
    }
}