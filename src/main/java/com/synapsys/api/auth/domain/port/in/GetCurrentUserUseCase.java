package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.User;

import java.util.UUID;

public interface GetCurrentUserUseCase {
    User getCurrentUser(UUID userId);
}