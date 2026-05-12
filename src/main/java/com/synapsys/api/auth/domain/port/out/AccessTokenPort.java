package com.synapsys.api.auth.domain.port.out;

import com.synapsys.api.auth.domain.model.User;

public interface AccessTokenPort {
    String generate(User user);
}