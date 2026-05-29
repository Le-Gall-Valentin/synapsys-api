package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.ConfirmTotpCommand;

public interface ConfirmTotpUseCase {
    void confirm(ConfirmTotpCommand command);
}