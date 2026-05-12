package com.synapsys.api.auth.domain.port.in;

import com.synapsys.api.auth.domain.model.AuthTokens;

public interface RefreshTokenUseCase {
    AuthTokens refresh(String rawRefreshToken);
}