package com.synapsys.api.auth.domain.port.out;

import com.synapsys.api.auth.domain.model.User;

public interface RefreshTokenIssuerPort {
    String generate(User user, int expiryDays);
}