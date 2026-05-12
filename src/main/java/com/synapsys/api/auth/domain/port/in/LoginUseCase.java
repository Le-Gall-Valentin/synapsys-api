package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.AuthTokens;
import com.synapsys.api.auth.domain.model.LoginCommand;

public interface LoginUseCase {
    AuthTokens login(LoginCommand command);
}