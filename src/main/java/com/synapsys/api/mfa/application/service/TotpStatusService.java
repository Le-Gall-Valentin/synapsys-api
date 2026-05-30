package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import java.util.UUID;

@ApplicationService
public class TotpStatusService {

    private final UserTotpQueryPort userTotpQuery;

    public TotpStatusService(UserTotpQueryPort userTotpQuery) {
        this.userTotpQuery = userTotpQuery;
    }

    public boolean isTotpEnabled(UUID userId) {
        return userTotpQuery.findById(userId)
            .map(UserTotpProfile::totpEnabled)
            .orElse(false);
    }
}