package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.SetupTotpCommand;
import com.synapsys.api.auth.domain.model.TotpSetupResult;

public interface SetupTotpUseCase {
    TotpSetupResult setup(SetupTotpCommand command);
}