package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.DisableTotpCommand;

public interface DisableTotpUseCase {
    void disable(DisableTotpCommand command);
}