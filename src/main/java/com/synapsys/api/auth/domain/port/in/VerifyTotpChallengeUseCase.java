package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.LoginResult;
import com.synapsys.api.auth.domain.model.VerifyTotpChallengeCommand;

public interface VerifyTotpChallengeUseCase {
    LoginResult.Success verify(VerifyTotpChallengeCommand command);
}