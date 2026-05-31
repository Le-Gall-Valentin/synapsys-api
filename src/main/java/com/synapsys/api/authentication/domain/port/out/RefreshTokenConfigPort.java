package com.synapsys.api.authentication.domain.port.out;

public interface RefreshTokenConfigPort {
    int refreshTokenExpiryDays();
    String refreshTokenPurgeCron();
}