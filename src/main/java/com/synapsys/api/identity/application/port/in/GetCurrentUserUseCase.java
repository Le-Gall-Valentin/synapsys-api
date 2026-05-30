package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.User;

import java.util.UUID;

public interface GetCurrentUserUseCase {
    User getCurrentUser(UUID userId);
}