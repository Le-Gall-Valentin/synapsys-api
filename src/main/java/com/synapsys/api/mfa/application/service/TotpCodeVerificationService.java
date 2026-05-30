package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class TotpCodeVerificationService {

    private final UserTotpQueryPort userTotpQuery;
    private final TotpCodeValidatorPort codeValidator;
    private final TotpCodeReplayPort codeReplay;

    public TotpCodeVerificationService(UserTotpQueryPort userTotpQuery,
                                       TotpCodeValidatorPort codeValidator,
                                       TotpCodeReplayPort codeReplay) {
        this.userTotpQuery = userTotpQuery;
        this.codeValidator = codeValidator;
        this.codeReplay = codeReplay;
    }

    public boolean verifyAndConsume(UUID userId, String code) {
        Optional<UserTotpProfile> profileOpt = userTotpQuery.findById(userId);
        if (profileOpt.isEmpty()) return false;
        UserTotpProfile profile = profileOpt.get();
        if (profile.totpSecret() == null) return false;
        if (!codeValidator.isValid(profile.totpSecret(), code)) return false;
        return codeReplay.markCodeUsedIfAbsent(userId, code);
    }
}