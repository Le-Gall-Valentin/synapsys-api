package com.synapsys.api.auth.domain.port.out;

public interface TotpSecretGeneratorPort {
    String generateSecret();
    String buildOtpauthUri(String secret, String email);
}