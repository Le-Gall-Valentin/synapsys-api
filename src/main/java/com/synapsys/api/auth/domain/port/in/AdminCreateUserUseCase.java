package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.AdminCreateUserCommand;
import com.synapsys.api.auth.domain.model.User;

public interface AdminCreateUserUseCase {
    User create(AdminCreateUserCommand command);
}