package com.synapsys.api.auth.infrastructure.config;

import com.synapsys.api.auth.domain.port.out.RefreshTokenConfigPort;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenConfigAdapter implements RefreshTokenConfigPort {

    private final SynapsysProperties properties;

    public RefreshTokenConfigAdapter(SynapsysProperties properties) {
        this.properties = properties;
    }

    @Override
    public int refreshTokenExpiryDays() {
        return properties.refreshToken().expiryDays();
    }

    @Override
    public String refreshTokenPurgeCron() {
        return properties.refreshToken().purgeCron();
    }
}