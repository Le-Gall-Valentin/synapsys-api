package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.ResetUserTotpCommand;

public interface ResetUserTotpUseCase {
    void reset(ResetUserTotpCommand command);
}