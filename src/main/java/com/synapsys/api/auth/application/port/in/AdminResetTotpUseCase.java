package com.synapsys.api.auth.application.port.in;

import com.synapsys.api.auth.application.dto.AdminResetTotpCommand;

public interface AdminResetTotpUseCase {
    void reset(AdminResetTotpCommand command);
}
