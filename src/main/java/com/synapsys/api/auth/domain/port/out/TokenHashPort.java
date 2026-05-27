package com.synapsys.api.auth.domain.port.out;

public interface TokenHashPort {
    String hash(String rawValue);
}
