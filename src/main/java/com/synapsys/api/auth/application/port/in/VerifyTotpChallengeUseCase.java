package com.synapsys.api.auth.application.port.in;

import com.synapsys.api.auth.application.dto.VerifyTotpChallengeCommand;
import com.synapsys.api.auth.domain.model.LoginResult;

public interface VerifyTotpChallengeUseCase {
    LoginResult.Success verify(VerifyTotpChallengeCommand command);
}