package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.port.out.MfaAdminResetTotpPort;
import com.synapsys.api.mfa.application.service.AdminTotpDisableService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MfaAdminResetTotpAdapter implements MfaAdminResetTotpPort {

    private final AdminTotpDisableService adminTotpDisableService;

    public MfaAdminResetTotpAdapter(AdminTotpDisableService adminTotpDisableService) {
        this.adminTotpDisableService = adminTotpDisableService;
    }

    @Override
    public void disableTotpIfEnabled(UUID userId) {
        adminTotpDisableService.disableIfEnabled(userId);
    }
}