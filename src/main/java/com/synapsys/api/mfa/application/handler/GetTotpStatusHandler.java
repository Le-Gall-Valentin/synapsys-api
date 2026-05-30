package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.port.in.GetTotpStatusUseCase;
import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import com.synapsys.api.shared.annotation.ApplicationService;

import java.util.UUID;

@ApplicationService
public class GetTotpStatusHandler implements GetTotpStatusUseCase {

    private final UserTotpQueryPort userTotpQuery;

    public GetTotpStatusHandler(UserTotpQueryPort userTotpQuery) {
        this.userTotpQuery = userTotpQuery;
    }

    @Override
    public boolean isTotpEnabled(UUID userId) {
        return userTotpQuery.findById(userId)
            .map(UserTotpProfile::totpEnabled)
            .orElse(false);
    }
}