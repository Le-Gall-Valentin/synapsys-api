package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.domain.port.out.UserTotpPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import java.util.UUID;

@ApplicationService
public class TotpRecordInitService {

    private final UserTotpPort userTotpPort;

    public TotpRecordInitService(UserTotpPort userTotpPort) {
        this.userTotpPort = userTotpPort;
    }

    public void initForNewUser(UUID userId) {
        userTotpPort.createDefaultRecord(userId);
    }
}