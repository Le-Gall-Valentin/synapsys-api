package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String hash(String raw) {
        return encoder.encode(raw);
    }

    @Override
    public boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}