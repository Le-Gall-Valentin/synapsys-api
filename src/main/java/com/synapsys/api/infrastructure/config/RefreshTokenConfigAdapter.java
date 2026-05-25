package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.port.out.RefreshTokenConfigPort;
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
}