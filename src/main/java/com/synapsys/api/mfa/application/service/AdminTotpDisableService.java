package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.application.port.in.AdminDisableTotpUseCase;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@ApplicationService
public class AdminTotpDisableService implements AdminDisableTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final UserTotpPort userTotpPort;

    public AdminTotpDisableService(UserTotpQueryPort userTotpQuery, UserTotpPort userTotpPort) {
        this.userTotpQuery = userTotpQuery;
        this.userTotpPort = userTotpPort;
    }

    @Transactional
    public void disableIfEnabled(UUID userId) {
        userTotpQuery.findById(userId).ifPresent(profile -> {
            if (profile.totpEnabled()) userTotpPort.disableTotp(userId);
        });
    }
}