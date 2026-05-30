package com.synapsys.api.mfa.application.port.in;

import com.synapsys.api.mfa.application.dto.ResetUserTotpCommand;

public interface ResetUserTotpUseCase {
    void reset(ResetUserTotpCommand command);
}