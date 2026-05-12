package com.synapsys.api.auth.domain.port.in;

public interface LogoutUseCase {
    void logout(String rawRefreshToken);
}