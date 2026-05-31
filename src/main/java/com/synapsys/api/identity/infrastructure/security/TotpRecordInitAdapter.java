package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.identity.domain.port.out.TotpRecordInitPort;
import com.synapsys.api.mfa.application.service.TotpRecordInitService;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TotpRecordInitAdapter implements TotpRecordInitPort {

    private final TotpRecordInitService totpRecordInitService;

    public TotpRecordInitAdapter(TotpRecordInitService totpRecordInitService) {
        this.totpRecordInitService = totpRecordInitService;
    }

    @Override
    public void initForUser(UUID userId) {
        totpRecordInitService.initForNewUser(userId);
    }
}