package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.application.dto.LoginCommand;
import com.synapsys.api.auth.domain.model.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand command);
}