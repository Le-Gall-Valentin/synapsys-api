package com.synapsys.api.auth.domain.port.out;

public interface PasswordHasherPort {
    String hash(String raw);
    boolean matches(String raw, String hash);
}