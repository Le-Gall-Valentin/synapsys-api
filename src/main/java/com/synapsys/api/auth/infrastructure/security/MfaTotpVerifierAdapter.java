package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.port.out.MfaTotpVerifierPort;
import com.synapsys.api.mfa.application.service.TotpCodeVerificationService;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class MfaTotpVerifierAdapter implements MfaTotpVerifierPort {

    private final TotpCodeVerificationService verificationService;

    public MfaTotpVerifierAdapter(TotpCodeVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Override
    public boolean verifyAndConsume(UUID userId, String code) {
        return verificationService.verifyAndConsume(userId, code);
    }
}